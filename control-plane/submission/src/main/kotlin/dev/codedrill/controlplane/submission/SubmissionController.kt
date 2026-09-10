package dev.codedrill.controlplane.submission

import com.fasterxml.jackson.databind.ObjectMapper
import dev.codedrill.judge.protocol.Language
import dev.codedrill.controlplane.submission.trace.Divergence
import dev.codedrill.controlplane.submission.trace.DivergenceService
import dev.codedrill.controlplane.submission.trace.PredictionService
import dev.codedrill.controlplane.submission.trace.StatePrediction
import dev.codedrill.controlplane.submission.trace.TraceRepository
import dev.codedrill.judge.protocol.TraceChunk
import dev.codedrill.judge.protocol.TraceManifest
import dev.codedrill.judge.protocol.Verdict
import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Page
import dev.codedrill.platform.common.Principal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

/**
 * 제출 API (기술 설계서 §9.2, §11.4 객체 소유권).
 *
 * 호출자는 Identity 가 토큰에서 확인한 주체다. 자칭한 헤더가 아니다.
 *
 * **남의 제출은 없는 것처럼 보인다.** 소유자가 아닐 때 403 이 아니라 404 를 내는 것은
 * 의도다 — 403 은 "그 제출은 있는데 네 것이 아니다"를 알려 주고, 그것만으로 ID 를
 * 훑어 누가 무엇을 언제 제출했는지 셀 수 있다 (§11.1 소스 노출, §11.3 비공개 기본).
 */
@RestController
@RequestMapping("/api/v1/submissions")
class SubmissionController(
    private val service: SubmissionService,
    private val events: SubmissionEventStream,
    private val traces: TraceRepository,
    private val divergence: DivergenceService,
    private val predictions: PredictionService,
    private val json: ObjectMapper,
    private val metrics: SubmissionMetrics,
) {

    @PostMapping
    fun create(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: CreateSubmissionRequest,
    ): ResponseEntity<SubmissionResponse> {
        // 트랜잭션 경계가 서비스에 있으므로, 여기서 감싸야 커밋까지가 측정에 들어간다
        // (§12.1 API 수신 → submission/outbox commit).
        val submission = metrics.timeAccept {
            service.create(
                CreateSubmission(
                    userId = principal.id,
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
    fun get(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<SubmissionResponse> {
        val submission = ownedBy(principal, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(SubmissionResponse.of(submission, json))
    }

    /**
     * 제출한 코드 (PRD §6.4).
     *
     * 목록과 상세에는 싣지 않고 **따로 받아 간다.** 기록 한 화면에 스무 건의 소스를 함께
     * 실으면 대부분 읽히지 않을 코드가 오가고, 소스는 그중 가장 민감한 값이다 (§11.1).
     * 사용자가 실제로 펼쳐 볼 때만 나간다.
     *
     * 소유자만 받을 수 있다. 남의 것은 없는 것처럼 답한다 — 404 와 403 을 가르면 그 자체로
     * "그 id 는 있다"를 알려 주는 셈이다.
     *
     * 계정을 지운 사용자의 소스는 비어 있다 (§11.3). 그때는 204 다 — 없는 것과 지운 것을
     * 화면에서 갈라 말할 수 있어야 한다.
     */
    @GetMapping("/{id}/source")
    fun source(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<Map<String, String>> {
        ownedBy(principal, id) ?: return ResponseEntity.notFound().build()
        val source = service.sourceOf(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(mapOf("source" to source))
    }

    /** 제출 기록 (§9.1). 정렬 키가 고정된 cursor 페이지네이션이다. */
    @GetMapping
    fun history(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @RequestParam(required = false) problemId: String?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
    ): Page<SubmissionResponse> {
        val page = service.history(principal.id, problemId, cursor, limit)
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
    fun traceManifest(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<TraceManifest> {
        ownedBy(principal, id) ?: return ResponseEntity.notFound().build()
        val manifest = traces.findManifest(id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(manifest)
    }

    /** 트레이스 청크 (§9.2 `GET /traces/{id}/chunks/{seq}`). */
    @GetMapping("/{id}/trace/chunks/{index}")
    fun traceChunk(
        @PathVariable id: UUID,
        @PathVariable index: Int,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<TraceChunk> {
        ownedBy(principal, id) ?: return ResponseEntity.notFound().build()
        val chunk = traces.findChunk(id, index) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(chunk)
    }

    /**
     * 최초 분기 진단 (PRD FR-805).
     *
     * 아직 계산되지 않았으면 204 다. 트레이스와 같은 이유로 오류가 아니다 — 참조 실행이
     * 아직 안 돌았거나, 이 문제에 참조 풀이가 없을 수 있다.
     *
     * **참조 코드도 참조 트레이스도 여기서 나가지 않는다.** 나가는 것은 갈라진 그 한
     * 이벤트를 사람 말로 옮긴 한 줄뿐이다 (§8.3).
     */
    @GetMapping("/{id}/divergence")
    fun divergenceOf(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<Divergence> {
        val found = divergence.find(principal.id, id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(found)
    }

    /**
     * 다음 상태 예측 (PRD FR-805).
     *
     * 채점은 서버가 한다. 클라이언트가 맞고 틀림을 정하면 그것은 채점이 아니라 자기
     * 신고이고, 증거로 쓸 수 없다.
     *
     * 자리를 **이벤트의 seq 로** 받는다. 화면의 위치는 클라이언트가 몇 개를 불러왔는지에
     * 달려 있어, 그것으로 채점하면 사용자가 본 자리와 채점한 자리가 어긋날 수 있다.
     */
    @PostMapping("/{id}/predictions")
    fun predict(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: PredictionRequest,
    ): ResponseEntity<Any> = when (
        val outcome = predictions.predict(principal.id, id, request.seq, request.predicted, request.rationale)
    ) {
        is PredictionService.Outcome.Graded -> ResponseEntity.ok(outcome.prediction)

        // 이미 맞혀 본 자리다. 두 번째는 예측이 아니라 받아쓰기다.
        PredictionService.Outcome.AlreadyAnswered ->
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError(ErrorCode.CONTENT_UNAVAILABLE, "이미 맞혀 본 자리다", UUID.randomUUID().toString()))

        PredictionService.Outcome.NoSuchStep ->
            ResponseEntity.badRequest()
                .body(ApiError(ErrorCode.INVALID_SIGNATURE, "그런 이벤트가 없다", UUID.randomUUID().toString()))

        PredictionService.Outcome.NotFound -> ResponseEntity.notFound().build()
    }

    /** 이 제출에서 이미 맞혀 본 자리들. 화면이 같은 자리를 다시 묻지 않게 한다. */
    @GetMapping("/{id}/predictions")
    fun predictions(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<List<StatePrediction>> =
        ResponseEntity.ok(predictions.answered(principal.id, id))

    /**
     * 판정 이력 (§4.2 INV-02).
     *
     * 재채점으로 점수가 바뀐 사용자가 "왜 바뀌었나"에 답을 얻는 곳이다. 최초 판정부터
     * 전부 들어 있어야 무엇에서 무엇으로 바뀌었는지 스스로 볼 수 있다.
     */
    @GetMapping("/{id}/judgements")
    fun judgements(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<List<Judgement>> {
        ownedBy(principal, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(service.judgements(id))
    }

    /**
     * 상태 스트림.
     *
     * 구독 직후 현재 상태를 한 번 흘려보내, 구독 이전에 지나간 전이를 놓친 클라이언트도
     * 즉시 수렴한다. 남의 제출이면 구독조차 만들지 않는다 — 스트림을 열어 두면 판정이
     * 언제 끝났는지가 새어 나간다.
     */
    @GetMapping("/{id}/events")
    fun events(
        @PathVariable id: UUID,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
    ): ResponseEntity<SseEmitter> {
        val current = ownedBy(principal, id) ?: return ResponseEntity.notFound().build()

        val emitter = events.subscribe(id.toString())
        emitter.send(SseEmitter.event().name("status").data(SubmissionResponse.of(current, json)))
        return ResponseEntity.ok(emitter)
    }

    /**
     * 호출자의 제출이면 돌려주고, 아니면 null.
     *
     * "없다"와 "네 것이 아니다"를 호출부에서 같게 다루도록 한 곳에 모은다. 두 경우를
     * 다르게 응답하는 순간 존재 여부가 새어 나간다.
     */
    private fun ownedBy(principal: Principal, id: UUID): Submission? =
        service.find(id)?.takeIf { it.userId == principal.id }

    /**
     * 쿼터 초과는 429 다 (§9.4).
     *
     * 400 이 아닌 이유는 **요청이 잘못된 것이 아니기 때문**이다. 같은 요청을 조금 뒤에
     * 보내면 통과한다. 클라이언트가 고칠 것과 기다릴 것을 상태 코드로 가른다.
     */
    @ExceptionHandler(QuotaExceededException::class)
    fun onQuota(e: QuotaExceededException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
            ApiError(ErrorCode.QUOTA_EXCEEDED, e.message ?: "쿼터를 넘겼다", traceId()),
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun onInvalid(e: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(
            ApiError(ErrorCode.INVALID_SIGNATURE, e.message ?: "요청이 유효하지 않다", traceId()),
        )

    private fun traceId(): String = UUID.randomUUID().toString()
}

/**
 * 예측 한 건 (FR-805).
 *
 * [rationale] 은 선택이다. 필수로 하면 예측 자체를 건너뛰고, 그러면 아무 기록도 남지
 * 않는다 — 풀이 전 질문이 같은 이유로 같은 모양을 쓴다 (FR-803).
 */
data class PredictionRequest(
    @field:Positive val seq: Int,
    @field:NotBlank val predicted: String,
    val rationale: String? = null,
)

data class CreateSubmissionRequest(
    @field:NotBlank val problemId: String,
    /**
     * 채점할 문제 버전. **1 이상이어야 한다.**
     *
     * 빠뜨리면 Jackson 이 Int 기본값 0 을 채운다. 그대로 받으면 존재하지 않는 버전을
     * 가리키는 제출이 만들어지고, 큐에 실려 오케스트레이터에서 영원히 실패한다 —
     * 잘못된 요청 하나가 소비자를 재시도 루프에 가둔다. 여기서 400 으로 끊는다.
     */
    @field:Positive val problemVersion: Int,
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
