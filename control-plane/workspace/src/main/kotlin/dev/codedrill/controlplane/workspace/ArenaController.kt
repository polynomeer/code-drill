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

/** 반례 아레나 API (§8.3). 폴링이다 — 시도 하나가 몇 초에서 수십 초다. */
@RestController
@RequestMapping("/api/v1/arena")
class ArenaController(private val service: ArenaService, private val community: CommunityMutantService) {

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

    // --- 남의 오답 세우기 (§8.3, §8.5) ---

    /** 내놓을 수 있는 내 오답. 틀린 사람에게도 열린다 — 내놓는 것과 깨뜨리는 것은 다른 일이다. */
    @GetMapping("/{problemId}/donatable")
    fun donatable(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): List<DonatableSubmission> = community.donatable(principal.id, problemId)

    @GetMapping("/{problemId}/donations/mine")
    fun myDonations(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): List<ArenaDonation> = community.mine(principal.id, problemId).map { it.withoutSource() }

    @PostMapping("/{problemId}/donations")
    fun donate(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
        @Valid @RequestBody request: DonationRequest,
    ): ResponseEntity<Any> = when (val outcome = community.donate(principal.id, request.submissionId, request.note)) {
        is CommunityMutantService.DonateOutcome.Accepted -> ResponseEntity.accepted().body(outcome.donation.withoutSource())
        is CommunityMutantService.DonateOutcome.Duplicate ->
            ResponseEntity.status(HttpStatus.CONFLICT).body(outcome.donation.withoutSource())
        is CommunityMutantService.DonateOutcome.Invalid ->
            ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
    }

    /** 세워진 남의 오답을 신고한다. 맞힌 사람만 — 과녁을 본 사람만 할 수 있는 일이다. */
    @PostMapping("/{problemId}/targets/{name}/reports")
    fun report(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
        @PathVariable name: String,
        @Valid @RequestBody request: ReportRequest,
    ): ResponseEntity<Any> = when (val outcome = community.report(principal.id, problemId, name, request.reason)) {
        is CommunityMutantService.ReportOutcome.Filed -> ResponseEntity.accepted().body(outcome.report)
        CommunityMutantService.ReportOutcome.AlreadyFiled -> ResponseEntity.noContent().build()
        CommunityMutantService.ReportOutcome.Locked ->
            ResponseEntity.status(HttpStatus.CONFLICT).body(error(ErrorCode.CONTENT_UNAVAILABLE, "이 문제를 맞힌 뒤에 열린다"))
        is CommunityMutantService.ReportOutcome.Invalid ->
            ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
    }

    /** 기부자에게 돌려줄 때는 소스를 뺀다. 자기 것이지만 제출 화면에 이미 있고, 응답에 코드가 실리는 길을 하나라도 줄인다. */
    private fun ArenaDonation.withoutSource() = copy(source = null)

    private fun error(code: ErrorCode, message: String) = ApiError(code, message, UUID.randomUUID().toString())
}

data class DonationRequest(val submissionId: UUID, @field:NotBlank val note: String)

data class ReportRequest(@field:NotBlank val reason: String)

data class ArenaAttemptRequest(@field:NotEmpty val args: List<Any>)
