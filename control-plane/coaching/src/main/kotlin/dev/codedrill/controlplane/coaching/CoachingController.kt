package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.Competency
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 코칭 API (PRD FR-802).
 *
 * 힌트 본문은 **펼칠 때만** 응답에 실린다. 세션 조회가 사다리를 통째로 내려보내면
 * 화면에서 아무리 가려도 그것은 도움이 아니라 정답이다.
 */
@RestController
@RequestMapping("/api/v1/coaching")
class CoachingController(
    private val service: CoachingService,
    private val transfers: TransferService,
) {

    /** 세션을 연다. 이미 열려 있으면 그것을 돌려준다 — 두 번 눌러도 하나다. */
    @PostMapping("/sessions")
    fun open(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: OpenSessionRequest,
    ): ResponseEntity<SessionResponse> {
        val session = service.open(principal.id, request.problemId)
        return ResponseEntity.ok(response(session))
    }

    @GetMapping("/sessions/{id}")
    fun find(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<SessionResponse> {
        // 남의 세션은 없는 것처럼 답한다 (§11.1).
        val session = service.find(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(response(session))
    }

    @PostMapping("/sessions/{id}/reveal")
    fun reveal(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: RevealRequest,
    ): ResponseEntity<Any> {
        val competency = Competency.entries.firstOrNull { it.name == request.competency }
            ?: return ResponseEntity.badRequest()
                .body(error(ErrorCode.INVALID_SIGNATURE, "그런 역량이 없다: ${request.competency}"))

        return when (val outcome = service.reveal(principal.id, id, competency)) {
            is CoachingService.Outcome.Revealed ->
                ResponseEntity.ok(response(outcome.session))

            is CoachingService.Outcome.Exhausted ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    error(
                        ErrorCode.CONTENT_UNAVAILABLE,
                        "이 역량에는 ${outcome.total}단계까지만 있다",
                    ),
                )

            // 초점 밖은 없는 것처럼 답하지 않는다. 사용자가 무엇을 눌렀는지 알고 있고,
            // "이번 세션은 그것을 다루지 않는다"가 정확한 사실이다.
            CoachingService.Outcome.OutOfFocus ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error(ErrorCode.CONTENT_UNAVAILABLE, "이번 세션이 다루는 역량이 아니다"))

            CoachingService.Outcome.Closed ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error(ErrorCode.CONTENT_UNAVAILABLE, "이미 끝난 세션이다"))

            CoachingService.Outcome.NotFound ->
                ResponseEntity.notFound().build()
        }
    }

    /**
     * 전이 확인 과제를 받는다 (FR-807).
     *
     * 도움을 하나도 받지 않았으면 과제가 없다. 스스로 푼 것은 이미 그 자체로 증거이고,
     * 그 위에 과제를 얹으면 안 받아도 될 숙제가 된다.
     */
    @PostMapping("/sessions/{id}/transfer")
    fun assign(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = when (val outcome = transfers.assign(principal.id, id)) {
        is TransferService.Outcome.Assigned -> ResponseEntity.ok(TransferView.of(outcome.task))

        TransferService.Outcome.NothingToTransfer ->
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.CONTENT_UNAVAILABLE, "도움을 받지 않았으니 확인할 전이가 없다"))

        TransferService.Outcome.NoTarget ->
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.CONTENT_UNAVAILABLE, "같은 역량을 요구하는 안 푼 문제가 없다"))

        else -> ResponseEntity.notFound().build()
    }

    @GetMapping("/sessions/{id}/transfer")
    fun transfer(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<TransferView> {
        val task = transfers.ofSession(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TransferView.of(task))
    }

    /** 설명 과제를 낸다. 이것을 내야 뒤따르는 판정이 전이 확인이 된다. */
    @PostMapping("/transfers/{id}/explain")
    fun explain(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ExplainRequest,
    ): ResponseEntity<Any> = when (val outcome = transfers.explain(principal.id, id, request.text)) {
        is TransferService.Outcome.Assigned -> ResponseEntity.ok(TransferView.of(outcome.task))

        is TransferService.Outcome.TooShort ->
            ResponseEntity.badRequest().body(
                error(
                    ErrorCode.INVALID_SIGNATURE,
                    "${outcome.required}자 이상 적어 주세요. 자기 말로 정리하는 것이 이 과제입니다",
                ),
            )

        TransferService.Outcome.AlreadyExplained ->
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.CONTENT_UNAVAILABLE, "이미 설명을 냈다"))

        else -> ResponseEntity.notFound().build()
    }

    @PostMapping("/sessions/{id}/close")
    fun close(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> =
        if (service.close(principal.id, id)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()

    private fun response(session: CoachingSession): SessionResponse {
        val remaining = service.remaining(session)
        return SessionResponse(
            id = session.id.toString(),
            problemId = session.problemId,
            focus = session.focus.map { FocusView(it.name, remaining[it] ?: 0) },
            // 이미 펼친 것은 본문째 돌려준다. 다시 보려고 다음 단계를 펼치게 하면,
            // 사용자는 필요하지도 않은 도움을 받고 그만큼 증거가 가벼워진다.
            revealed = service.revealedTexts(session).map { (assistance, hint) ->
                RevealedView(assistance.competency.name, hint.level, hint.text)
            },
            helpLevel = session.deepestLevel(),
            closed = session.endedAt != null,
        )
    }

    private fun error(code: ErrorCode, message: String) =
        ApiError(code, message, UUID.randomUUID().toString())
}

data class OpenSessionRequest(@field:NotBlank val problemId: String)
data class RevealRequest(@field:NotBlank val competency: String)

data class SessionResponse(
    val id: String,
    val problemId: String,
    /** 비어 있을 수 있다. 약한 역량이 없으면 도울 것도 없고, 그 사실을 말해야 한다. */
    val focus: List<FocusView>,
    val revealed: List<RevealedView>,
    /** 이 세션에서 가장 깊이 본 단계. 0 이면 도움 없이 풀고 있다는 뜻이다. */
    val helpLevel: Int,
    val closed: Boolean,
)

/** 아직 남은 단계 수만 알려 준다. 본문은 펼쳐야 온다. */
data class FocusView(val competency: String, val remaining: Int)

data class RevealedView(val competency: String, val level: Int, val text: String)

data class ExplainRequest(@field:NotBlank val text: String)

/**
 * 전이 확인 과제 (FR-807).
 *
 * 변형 문제의 아이디만 준다. 왜 이 문제인지를 함께 적으면 그것이 곧 접근을 알려 주는
 * 힌트가 된다 — "같은 역량을 요구한다"는 말은 "같은 생각으로 풀린다"는 뜻이다.
 */
data class TransferView(
    val id: String,
    val sourceProblemId: String,
    val targetProblemId: String,
    val explanation: String?,
    val status: TransferStatus,
) {
    companion object {
        fun of(task: TransferTask) = TransferView(
            id = task.id.toString(),
            sourceProblemId = task.sourceProblemId,
            targetProblemId = task.targetProblemId,
            explanation = task.explanation,
            status = task.status,
        )
    }
}
