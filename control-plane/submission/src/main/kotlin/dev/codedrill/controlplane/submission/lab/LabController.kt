package dev.codedrill.controlplane.submission.lab

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import jakarta.validation.Valid
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
 * 해설·실험실 API (§6.4~6.6, FR-214).
 *
 * `/api/v1/labs` 아래다. 문제 경로(/problems 아래)는 로그인 없이도 열리는데, 해설은
 * 누가 열었는지가 곧 기록이라 반드시 로그인이어야 한다 (FR-214 열람 이력).
 */
@RestController
@RequestMapping("/api/v1/labs")
class LabController(private val service: LabService) {

    @GetMapping("/{problemId}/editorial")
    fun editorial(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): EditorialView = service.editorial(principal.id, problemId)

    /** 정답 전에 연다. 되돌릴 수 없고, 그 뒤 제출의 증거가 가벼워진다 (FR-214). */
    @PostMapping("/{problemId}/editorial/unlock")
    fun unlock(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): EditorialView = service.unlock(principal.id, problemId)

    @PostMapping("/{problemId}/runs")
    fun start(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
        @Valid @RequestBody request: LabRunRequest,
    ): ResponseEntity<Any> = when (val outcome = service.start(principal.id, problemId, request.args, request.labels)) {
        is LabService.Outcome.Started -> ResponseEntity.accepted().body(outcome.run)

        LabService.Outcome.Locked ->
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.CONTENT_UNAVAILABLE, "맞히거나 해설을 열어야 실험실을 쓸 수 있다"))

        is LabService.Outcome.Invalid ->
            ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))

        is LabService.Outcome.Throttled ->
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                error(ErrorCode.QUOTA_EXCEEDED, "한 시간에 ${outcome.allowed}번까지 돌릴 수 있다 (${outcome.used}번 썼다)"),
            )
    }

    @GetMapping("/runs/{id}")
    fun find(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<LabRun> {
        val run = service.find(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(run)
    }

    private fun error(code: ErrorCode, message: String) = ApiError(code, message, UUID.randomUUID().toString())
}

data class LabRunRequest(
    @field:NotEmpty val args: List<Any>,
    @field:NotEmpty val labels: List<String>,
)
