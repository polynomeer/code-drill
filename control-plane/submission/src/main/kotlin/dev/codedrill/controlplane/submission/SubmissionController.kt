package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.Language
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
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
    private val json: ObjectMapper,
) {

    @PostMapping
    fun create(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
        @RequestBody request: CreateSubmissionRequest,
    ): ResponseEntity<SubmissionResponse> {
        val submission = service.create(
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
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SubmissionResponse.of(submission, json))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<SubmissionResponse> {
        val submission = service.find(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(SubmissionResponse.of(submission, json))
    }

    @GetMapping
    fun recent(
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
    ): List<SubmissionResponse> = service.recent(userId).map { SubmissionResponse.of(it, json) }

    /**
     * 상태 스트림. 구독 직후 현재 상태를 한 번 흘려보내, 구독 이전에 지나간 전이를
     * 놓친 클라이언트도 즉시 수렴한다.
     */
    /** 실행 트레이스. 아직 준비되지 않았으면 204 다 — 오류가 아니다 (§12.2). */
    @GetMapping("/{id}/trace")
    fun trace(@PathVariable id: UUID): ResponseEntity<Any> {
        val trace = service.trace(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(json.readTree(trace))
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
        )
    }
}
