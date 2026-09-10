package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
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
 * 시험 실행 API (기획서 부록 A 실행 도메인).
 *
 * 결과는 폴링으로 가져간다. 제출은 SSE 로 미는데, 시험 실행은 몇 초면 끝나고 화면 하나만
 * 보고 있으므로 연결을 하나 더 여는 값을 하지 못한다.
 */
@RestController
@RequestMapping("/api/v1/trials")
class TrialController(private val service: TrialService) {

    @PostMapping
    fun start(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: TrialRequest,
    ): ResponseEntity<Any> =
        when (
            val outcome = service.start(
                userId = principal.id,
                problemId = request.problemId,
                language = request.language.uppercase(),
                source = request.source,
                cases = request.cases.map { TrialCase(it.args, it.expected) },
            )
        ) {
            is TrialService.Outcome.Started ->
                ResponseEntity.accepted().body(TrialResponse.of(outcome.trial))

            is TrialService.Outcome.Invalid ->
                ResponseEntity.badRequest()
                    .body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))

            is TrialService.Outcome.Throttled ->
                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    error(
                        ErrorCode.QUOTA_EXCEEDED,
                        "한 시간에 ${outcome.allowed}번까지 돌릴 수 있다 (${outcome.used}번 썼다)",
                    ),
                )
        }

    @GetMapping("/{id}")
    fun find(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<TrialResponse> {
        // 남의 실행은 없는 것처럼 답한다. 404 와 403 을 가르면 그 자체로 "그 id 는 있다"를
        // 알려 주는 셈이다 (§11.1).
        val trial = service.find(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TrialResponse.of(trial))
    }

    private fun error(code: ErrorCode, message: String) =
        ApiError(code, message, UUID.randomUUID().toString())
}

data class TrialRequest(
    @field:NotBlank val problemId: String,
    @field:NotBlank val language: String,
    @field:NotBlank val source: String,
    @field:NotEmpty val cases: List<TrialCaseRequest>,
)

data class TrialCaseRequest(val args: List<Any>, val expected: Any? = null)

data class TrialResponse(
    val id: String,
    val problemId: String,
    val language: String,
    val status: TrialStatus,
    val compileLog: String?,
    val cases: List<TrialCaseResult>,
) {
    companion object {
        fun of(trial: TrialRun) = TrialResponse(
            id = trial.id.toString(),
            problemId = trial.problemId,
            language = trial.language,
            status = trial.status,
            compileLog = trial.compileLog,
            cases = trial.results,
        )
    }
}
