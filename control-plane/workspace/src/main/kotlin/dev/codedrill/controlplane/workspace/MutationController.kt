package dev.codedrill.controlplane.workspace

import dev.codedrill.judge.protocol.MutantOutcome
import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import dev.codedrill.platform.problempackage.DefectKind
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
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
 * 변이 평가 API (PRD FR-804).
 *
 * 시험 실행과 같이 폴링으로 가져간다. 다만 이쪽이 훨씬 오래 걸린다 — 정답 한 번에
 * 오답 N 번이므로, 화면은 몇 초가 아니라 십수 초를 기다릴 각오를 해야 한다.
 */
@RestController
@RequestMapping("/api/v1/mutations")
class MutationController(private val service: MutationService) {

    @PostMapping
    fun start(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: MutationCheckRequest,
    ): ResponseEntity<Any> =
        when (
            val outcome = service.start(
                userId = principal.id,
                problemId = request.problemId,
                cases = request.cases.map { TrialCase(it.args, it.expected) },
            )
        ) {
            is MutationService.Outcome.Started ->
                ResponseEntity.accepted().body(MutationResponse.of(outcome.evaluation))

            is MutationService.Outcome.Invalid ->
                ResponseEntity.badRequest()
                    .body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))

            // 요청은 멀쩡하다. 이 문제로 잴 수 없을 뿐이므로 400 이 아니다 — 400 이면
            // 화면이 "당신의 입력이 잘못됐다"를 말하게 된다.
            is MutationService.Outcome.Unavailable ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error(ErrorCode.CONTENT_UNAVAILABLE, outcome.reason))

            is MutationService.Outcome.Throttled ->
                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    error(
                        ErrorCode.QUOTA_EXCEEDED,
                        "한 시간에 ${outcome.allowed}번까지 잴 수 있다 (${outcome.used}번 썼다)",
                    ),
                )
        }

    @GetMapping("/{id}")
    fun find(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<MutationResponse> {
        // 남의 평가는 없는 것처럼 답한다 (§11.1).
        val evaluation = service.find(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(MutationResponse.of(evaluation))
    }

    private fun error(code: ErrorCode, message: String) =
        ApiError(code, message, UUID.randomUUID().toString())
}

data class MutationCheckRequest(
    @field:NotBlank val problemId: String,
    @field:NotEmpty val cases: List<TrialCaseRequest>,
)

/**
 * 평가 결과.
 *
 * **오답의 이름도 소스도 없다.** 나가는 것은 결함군과 그것을 잡은 내 케이스 번호까지다.
 */
data class MutationResponse(
    val id: String,
    val problemId: String,
    val status: MutationRunStatus,
    val message: String?,
    val mistakenCases: List<Int>,
    /** 잡은 수 / 전체 수. 손으로 잡을 수 없는 결함군은 빠진다 ([DefectKind.PERFORMANCE]). */
    val score: Double?,
    val kinds: List<KindSummary>,
) {
    companion object {
        fun of(evaluation: MutationEvaluation): MutationResponse {
            val scored = evaluation.outcomes.filter { it.kind.reachableByHandWrittenCase }
            return MutationResponse(
                id = evaluation.id.toString(),
                problemId = evaluation.problemId,
                status = evaluation.status,
                message = evaluation.message,
                mistakenCases = evaluation.mistakenCases,
                score = scored.takeIf { it.isNotEmpty() }
                    ?.let { rows -> rows.count { it.killed }.toDouble() / rows.size },
                kinds = evaluation.outcomes.groupBy { it.kind }.map(::summarize),
            )
        }

        /**
         * 결함군 하나로 접는다.
         *
         * 오답 단위로 내보내지 않는 이유는 그것이 곧 "이 문제에는 이런 오답이 몇 개 있다"를
         * 알려 주기 때문이다. 무리 단위면 다음에 무엇을 시험할지는 알 수 있고, 정답의
         * 골격은 여전히 보이지 않는다.
         */
        private fun summarize(entry: Map.Entry<DefectKind, List<MutantOutcome>>) = KindSummary(
            kind = entry.key,
            label = entry.key.label,
            killed = entry.value.count { it.killed },
            total = entry.value.size,
            scored = entry.key.reachableByHandWrittenCase,
            killedBy = entry.value.flatMap { it.killedBy }.distinct().sorted(),
        )
    }
}

data class KindSummary(
    val kind: DefectKind,
    val label: String,
    val killed: Int,
    val total: Int,
    /** 점수에 들어가는지. 손으로 적는 케이스로 잡을 수 없는 종류는 빠진다. */
    val scored: Boolean,
    /** 이 결함군을 잡은 **내** 케이스 번호. 자기 입력이므로 돌려줘도 새로 알려 줄 것이 없다. */
    val killedBy: List<Int>,
)
