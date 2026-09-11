package dev.codedrill.controlplane.workspace

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

/** 반례 아레나 API (§8.3). 폴링이다 — 시도 하나가 몇 초에서 수십 초다. */
@RestController
@RequestMapping("/api/v1/arena")
class ArenaController(private val service: ArenaService) {

    @GetMapping("/{problemId}")
    fun board(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): ArenaBoard = service.board(principal.id, problemId)

    @PostMapping("/{problemId}/attempts")
    fun attempt(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
        @Valid @RequestBody request: ArenaAttemptRequest,
    ): ResponseEntity<Any> = when (val outcome = service.attempt(principal.id, problemId, request.args)) {
        is ArenaService.Outcome.Started -> ResponseEntity.accepted().body(outcome.attempt)
        ArenaService.Outcome.Locked ->
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(ErrorCode.CONTENT_UNAVAILABLE, "이 문제를 맞힌 뒤에 열린다"))
        is ArenaService.Outcome.Invalid ->
            ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
        is ArenaService.Outcome.Throttled ->
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error(ErrorCode.QUOTA_EXCEEDED, "한 시간에 ${outcome.allowed}번까지 (${outcome.used}번 썼다)"))
    }

    @GetMapping("/attempts/{id}")
    fun find(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<ArenaAttempt> {
        val attempt = service.find(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(attempt)
    }

    private fun error(code: ErrorCode, message: String) = ApiError(code, message, UUID.randomUUID().toString())
}

data class ArenaAttemptRequest(@field:NotEmpty val args: List<Any>)
