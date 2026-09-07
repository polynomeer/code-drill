package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.Language
import dev.codedrill.controlplane.submission.trace.TraceRepository
import dev.codedrill.judge.protocol.TraceChunk
import dev.codedrill.judge.protocol.TraceManifest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Page
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

/**
 * 제출 API (기술 설계서 §9.2).
 *
 * 슬라이스에는 로그인이 없다. 사용자 식별은 헤더로 받으며, Identity 모듈이 붙으면
 * 인증 주체에서 가져오도록 바꾼다.
 */
@RestController
@RequestMapping("/api/v1/submissions")
class SubmissionController(
    private val service: SubmissionService,
    private val events: SubmissionEventStream,
    private val traces: TraceRepository,
    private val json: ObjectMapper,
    private val metrics: SubmissionMetrics,
) {

    @PostMapping
    fun create(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
        @RequestBody request: CreateSubmissionRequest,
    ): ResponseEntity<SubmissionResponse> {
        // 트랜잭션 경계가 서비스에 있으므로, 여기서 감싸야 커밋까지가 측정에 들어간다
        // (§12.1 API 수신 → submission/outbox commit).
        val submission = metrics.timeAccept {
            service.create(
                CreateSubmission(
                    userId = userId,
                    idempotencyKey = idempotencyKey,
                    problemId = request.problemId,
                    problemVersion = request.problemVersion,
                    language = request.language,
                    source = request.source,
                    requestTrace = request.requestTrace,
                ),
            )
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SubmissionResponse.of(submission, json))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<SubmissionResponse> {
        val submission = service.find(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(SubmissionResponse.of(submission, json))
    }

    /** 제출 기록 (§9.1). 정렬 키가 고정된 cursor 페이지네이션이다. */
    @GetMapping
    fun history(
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
        @RequestParam(required = false) problemId: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
    ): Page<SubmissionResponse> {
        val page = service.history(userId, problemId, cursor, limit)
        return Page(page.items.map { SubmissionResponse.of(it, json) }, page.nextCursor)
    }

    /**
     * 상태 스트림. 구독 직후 현재 상태를 한 번 흘려보내, 구독 이전에 지나간 전이를
     * 놓친 클라이언트도 즉시 수렴한다.
     */
    /**
     * 트레이스 목차 (§9.2 `GET /traces/{id}/manifest`).
     *
     * 아직 준비되지 않았으면 204 다 — 오류가 아니다. 트레이스는 판정 뒤에 별도 작업으로
     * 만들어지고, 영영 오지 않을 수도 있다 (§12.2).
     */
    @GetMapping("/{id}/trace")
    fun traceManifest(@PathVariable id: UUID): ResponseEntity<TraceManifest> {
        val manifest = traces.findManifest(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(manifest)
    }

    /** 트레이스 청크 (§9.2 `GET /traces/{id}/chunks/{seq}`). */
    @GetMapping("/{id}/trace/chunks/{index}")
    fun traceChunk(
        @PathVariable id: UUID,
        @PathVariable index: Int,
    ): ResponseEntity<TraceChunk> {
        val chunk = traces.findChunk(id, index) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(chunk)
    }

    /**
     * 판정 이력 (§4.2 INV-02).
     *
     * 재채점으로 점수가 바뀐 사용자가 "왜 바뀌었나"에 답을 얻는 곳이다. 최초 판정부터
     * 전부 들어 있어야 무엇에서 무엇으로 바뀌었는지 스스로 볼 수 있다.
     */
    @GetMapping("/{id}/judgements")
    fun judgements(@PathVariable id: UUID): ResponseEntity<List<Judgement>> {
        service.find(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(service.judgements(id))
    }

    @GetMapping("/{id}/events")
    fun events(@PathVariable id: UUID): SseEmitter {
        val emitter = events.subscribe(id.toString())
        service.find(id)?.let { current ->
            emitter.send(
                SseEmitter.event().name("status").data(SubmissionResponse.of(current, json)),
            )
        }
        return emitter
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun onInvalid(e: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(ErrorCode.INVALID_SIGNATURE, e.message ?: "요청이 유효하지 않다", traceId()),
        )

    private fun traceId(): String = UUID.randomUUID().toString()
}

data class CreateSubmissionRequest(
    @field:NotBlank val problemId: String,
    val problemVersion: Int,
    val language: Language,
    @field:NotBlank val source: String,
    /** 판정 뒤 학습용 트레이스를 이어서 만들지 (§9.3). */
    val requestTrace: Boolean = true,
)

data class SubmissionResponse(
    val id: String,
    val problemId: String,
    val problemVersion: Int,
    val language: String,
    val status: SubmissionStatus,
    val verdict: Verdict?,
    val score: Int?,
    val compileLog: String?,
    val groups: Any?,
    /** 몇 번째 판정인지. 1 보다 크면 재채점을 거쳤다는 뜻이다 (§4.2). */
    val revision: Int,
) {
    companion object {
        fun of(submission: Submission, json: ObjectMapper) = SubmissionResponse(
            id = submission.id.toString(),
            problemId = submission.problemId,
            problemVersion = submission.problemVersion,
            language = submission.language,
            status = submission.status,
            verdict = submission.verdict,
            score = submission.score,
            compileLog = submission.compileLog,
            // 숨은 그룹의 케이스 내역은 실행 영역에서 이미 잘려 왔다. 여기서 다시
            // 채우지 않는다 (§9.1 DTO 단계에서 제거).
            groups = submission.groupsJson?.let { json.readTree(it) },
            revision = submission.revision,
        )
    }
}
