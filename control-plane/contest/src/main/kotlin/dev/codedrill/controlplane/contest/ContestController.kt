package dev.codedrill.controlplane.contest

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
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

/** 대회 API (§8.4). */
@RestController
@RequestMapping("/api/v1/contests")
class ContestController(private val service: ContestService) {

    @GetMapping
    fun list(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): List<ContestService.ContestSummary> = service.list(principal.id)

    /** 내 레이팅 (§8.4). 변화의 이력까지. */
    @GetMapping("/me/rating")
    fun rating(@RequestAttribute(Principal.ATTRIBUTE) principal: Principal): Rating = service.rating(principal.id)

    @GetMapping("/{id}")
    fun view(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<ContestService.ContestView> {
        val view = service.view(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(view)
    }

    /** 참가. 참가가 곧 이름 공개 동의다 — 화면이 그것을 미리 말한다. */
    @PostMapping("/{id}/join")
    fun join(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = when (val outcome = service.join(principal.id, principal.displayName, id)) {
        is ContestService.JoinOutcome.Joined -> ResponseEntity.ok(outcome.contest)
        ContestService.JoinOutcome.AlreadyJoined -> ResponseEntity.noContent().build()
        is ContestService.JoinOutcome.Invalid -> ResponseEntity.badRequest().body(error(outcome.reason))
    }

    /** 가상 참가 (§8.4). 끝난 대회를 같은 시간 조건으로 혼자 다시. */
    @PostMapping("/{id}/virtual")
    fun virtual(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = duel(service.virtual(principal.id, principal.displayName, id))

    @PostMapping("/duels")
    fun openDuel(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: OpenDuelRequest,
    ): ResponseEntity<Any> = duel(service.openDuel(principal.id, principal.displayName, request.problemId, request.minutes))

    @PostMapping("/duels/join")
    fun joinDuel(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @Valid @RequestBody request: JoinDuelRequest,
    ): ResponseEntity<Any> = duel(service.joinDuel(principal.id, principal.displayName, request.code))

    private fun duel(outcome: ContestService.DuelOutcome): ResponseEntity<Any> = when (outcome) {
        is ContestService.DuelOutcome.Opened -> ResponseEntity.status(HttpStatus.CREATED).body(mapOf("contest" to outcome.contest, "joinCode" to outcome.joinCode))
        is ContestService.DuelOutcome.Invalid -> ResponseEntity.badRequest().body(error(outcome.reason))
    }

    private fun error(message: String) = ApiError(ErrorCode.INVALID_SIGNATURE, message, UUID.randomUUID().toString())
}

data class OpenDuelRequest(@field:NotBlank val problemId: String, val minutes: Int = 30)

data class JoinDuelRequest(@field:NotBlank val code: String)
