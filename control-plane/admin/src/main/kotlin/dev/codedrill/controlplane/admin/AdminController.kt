package dev.codedrill.controlplane.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 운영 API (기술 설계서 §3.1 Admin, §9.2, §11.2).
 *
 * 행위자는 [AdminAuthInterceptor] 가 넣어 둔 계정 id 다. 컨트롤러는 자칭한 값을 받지
 * 않으며, 감사 로그의 actor 도 이 값이다.
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val publish: PublishService,
    private val rejudge: RejudgeService,
    private val audit: AuditLog,
    private val roles: AdminRoles,
    private val arena: ArenaModeration,
    private val discussion: DiscussionModeration,
    private val integrity: IntegrityModeration,
    private val sanctions: SanctionModeration,
    private val contests: ContestAdministration,
) {

    // --- 아레나 검수 (§8.3 익명화된 오답, §8.5 신고·검수) ---

    /**
     * 검수 큐. 기부된 오답의 소스가 실린다 — 검수자가 보는 것이 곧 검수다.
     *
     * 재채점 승인과 같은 REVIEWER 다. 둘 다 "다른 사람의 판정에 영향을 주는 결정을 한 번
     * 더 보는" 역할이고, 역할을 늘리면 부여 절차만 늘어난다.
     */
    @RequiresRole(AdminRole.REVIEWER)
    @GetMapping("/arena/queue")
    fun arenaQueue(): Any = arena.queue()

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/arena/donations/{id}/approve")
    fun approveDonation(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ApproveDonationRequest,
    ): ResponseEntity<Any> = decided(arena.approve(id, actor, request.kind, request.note)) {
        audit.record(AuditAction.ARENA_DONATION_APPROVED, id.toString(), actor, mapOf("kind" to request.kind))
    }

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/arena/donations/{id}/reject")
    fun rejectDonation(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ArchiveRequest,
    ): ResponseEntity<Any> = decided(arena.reject(id, actor, request.reason)) {
        audit.record(AuditAction.ARENA_DONATION_REJECTED, id.toString(), actor, mapOf("reason" to request.reason))
    }

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/arena/reports/{id}/resolve")
    fun resolveReport(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ResolveReportRequest,
    ): ResponseEntity<Any> = decided(arena.resolve(id, actor, request.retire, request.resolution)) {
        audit.record(
            if (request.retire) AuditAction.ARENA_DONATION_RETIRED else AuditAction.ARENA_REPORT_DISMISSED,
            id.toString(), actor, mapOf("resolution" to request.resolution),
        )
    }

    // --- 질문 게시판 검수 (§8.5 신고·제재) ---

    /** 신고된 글. 같은 REVIEWER 다 — "남의 것이 여러 사람에게 보이는" 결정은 한 역할이 본다. */
    @RequiresRole(AdminRole.REVIEWER)
    @GetMapping("/discussions/queue")
    fun discussionQueue(): Any = discussion.queue()

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/discussions/reports/{id}/resolve")
    fun resolveDiscussionReport(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ResolvePostReportRequest,
    ): ResponseEntity<Any> = decided(discussion.resolve(id, actor, request.hide, request.resolution)) {
        audit.record(
            if (request.hide) AuditAction.DISCUSSION_POST_HIDDEN else AuditAction.DISCUSSION_REPORT_DISMISSED,
            id.toString(), actor, mapOf("resolution" to request.resolution),
        )
    }

    // --- 유사도 신호 검수 (§11.4 부정행위 방어, §10.4 정책) ---

    /** 열린 신호. 두 소스가 실린다 — 결정은 사람이 나란히 보고 한다. 판정은 바뀌지 않는다. */
    @RequiresRole(AdminRole.REVIEWER)
    @GetMapping("/integrity/queue")
    fun integrityQueue(): Any = integrity.queue()

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/integrity/flags/{id}/resolve")
    fun resolveFlag(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ResolveFlagRequest,
    ): ResponseEntity<Any> = decided(integrity.resolve(id, actor, request.confirmed, request.note)) {
        audit.record(
            if (request.confirmed) AuditAction.SIMILARITY_CONFIRMED else AuditAction.SIMILARITY_DISMISSED,
            id.toString(), actor, mapOf("note" to (request.note ?: "")),
        )
    }

    // --- 제재와 이의 (§8.5 단계적 제재, §10.4 이의 절차) ---

    /** 계정에 닿는 결정은 SECURITY_ADMIN 이다. 근거 없는 제재는 없다 — evidence 가 신호나 신고를 가리킨다. */
    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/sanctions")
    fun issueSanction(
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: IssueSanctionRequest,
    ): ResponseEntity<Any> = decided(sanctions.issue(request.userId, request.kind, request.reason, request.evidence, request.days, actor)) {
        audit.record(AuditAction.SANCTION_ISSUED, request.userId, actor, mapOf("kind" to request.kind, "evidence" to request.evidence, "days" to (request.days ?: 0)))
    }

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/sanctions/{id}/lift")
    fun liftSanction(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Any> = decided(sanctions.lift(id, actor)) {
        audit.record(AuditAction.SANCTION_LIFTED, id.toString(), actor, emptyMap())
    }

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @GetMapping("/sanctions/appeals")
    fun appeals(): Any = sanctions.appeals()

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/sanctions/{id}/appeal/resolve")
    fun resolveAppeal(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ResolveAppealRequest,
    ): ResponseEntity<Any> = decided(sanctions.resolveAppeal(id, actor, request.uphold, request.note)) {
        audit.record(
            if (request.uphold) AuditAction.APPEAL_UPHELD else AuditAction.APPEAL_LIFTED,
            id.toString(), actor, mapOf("note" to (request.note ?: "")),
        )
    }

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @GetMapping("/sanctions/users/{userId}")
    fun sanctionHistory(@PathVariable userId: String): Any = sanctions.history(userId)

    // --- 대회 (§8.4) ---

    /** 만드는 것과 여는 것을 가른다 — 문제 공개와 같은 2인 원칙이다. */
    @RequiresRole(AdminRole.CONTENT_EDITOR)
    @PostMapping("/contests")
    fun createContest(
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: CreateContestRequest,
    ): ResponseEntity<Any> = decided(contests.create(actor, request.kind, request.title, request.problemIds, request.startsAt, request.endsAt)) {
        audit.record(AuditAction.CONTEST_CREATED, request.title, actor, mapOf("kind" to request.kind, "problems" to request.problemIds.joinToString(",")))
    }

    @RequiresRole(AdminRole.PUBLISHER)
    @PostMapping("/contests/{id}/publish")
    fun publishContest(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Any> = decided(contests.publish(id, actor)) {
        audit.record(AuditAction.CONTEST_PUBLISHED, id.toString(), actor, emptyMap())
    }

    private inline fun decided(decision: ArenaModeration.Decision, onDecided: () -> Unit): ResponseEntity<Any> {
        if (decision.rejected != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to decision.rejected))
        }
        onDecided()
        return ResponseEntity.ok(decision.result)
    }

    // --- 콘텐츠 수명주기 ---

    /** 검증을 통과한 버전을 등록한다 (§6.3 보고서 digest 를 함께 받는다). */
    @RequiresRole(AdminRole.CONTENT_EDITOR)
    @PostMapping("/problems/{problemId}/versions")
    fun registerVersion(
        @PathVariable problemId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: RegisterVersionRequest,
    ): ResponseEntity<Map<String, String>> =
        when (val outcome = publish.registerVersion(
            problemId, request.version, request.packageDigest, request.reportDigest,
            request.validatorVersion, actor,
        )) {
            is PublishService.RegisterOutcome.Registered ->
                ResponseEntity.ok(mapOf("versionId" to outcome.versionId))

            is PublishService.RegisterOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    /** 공개. 등록자와 다른 사람이어야 한다 (§11.2). */
    @RequiresRole(AdminRole.PUBLISHER)
    @PostMapping("/problems/{problemId}/publish")
    fun publishVersion(
        @PathVariable problemId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: PublishRequest,
    ): ResponseEntity<Map<String, String>> =
        when (val outcome = publish.publish(
            problemId, request.version, request.reportDigest, request.validatorVersion, actor,
        )) {
            is PublishService.PublishOutcome.Published ->
                ResponseEntity.ok(mapOf("publishedVersionId" to outcome.versionId))

            is PublishService.PublishOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    /**
     * 역할 부여 현황 (§11.2).
     *
     * 누가 무엇을 할 수 있는지는 감사의 출발점이다. 설정 파일에 적혀 있던 시절에는
     * 이 질문에 답하려면 운영 환경의 환경변수를 봐야 했다.
     */
    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @GetMapping("/operators")
    fun operators(): List<RoleGrant> = roles.grants()

    /**
     * 역할 부여를 요청한다. **이것만으로는 권한이 늘지 않는다** (§11.2).
     *
     * 응답의 `status` 가 `REQUESTED` 면 다른 SECURITY_ADMIN 의 승인이 남았다는 뜻이다.
     * 부트스트랩 구간(SECURITY_ADMIN 한 명)에서 동료를 만들 때만 그 자리에서 부여되며,
     * 그때는 `GRANTED` 가 온다.
     */
    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/operators/{userId}/roles")
    fun requestRole(
        @PathVariable userId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: RoleRequest,
    ): ResponseEntity<Map<String, Any>> {
        val role = request.parsed()
            ?: return ResponseEntity.badRequest().body(mapOf("reason" to "알 수 없는 역할이다: ${request.role}"))

        return respond(userId, role, roles.requestGrant(userId, role, actor, request.reason))
    }

    /** 승인을 기다리는 역할 부여 요청 (§11.2). */
    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @GetMapping("/role-requests")
    fun roleRequests(): List<GrantRequest> = roles.pendingGrants()

    /**
     * 요청을 승인한다. 승인이 곧 부여다.
     *
     * 승인자는 요청자와도, 역할을 받는 사람과도 달라야 한다 — 둘 중 하나라도 겹치면
     * 권한을 키우는 데 필요한 사람 수가 둘로 줄어든다 (§11.2).
     */
    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/role-requests/{requestId}/approve")
    fun approveRole(
        @PathVariable requestId: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Map<String, Any>> = respondTo(requestId, roles.approveGrant(requestId, actor))

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/role-requests/{requestId}/reject")
    fun rejectRole(
        @PathVariable requestId: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ArchiveRequest,
    ): ResponseEntity<Map<String, Any>> =
        respondTo(requestId, roles.rejectGrant(requestId, actor, request.reason))

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @DeleteMapping("/operators/{userId}/roles/{role}")
    fun revokeRole(
        @PathVariable userId: String,
        @PathVariable role: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Map<String, Any>> {
        val parsed = runCatching { AdminRole.valueOf(role.uppercase()) }.getOrNull()
            ?: return ResponseEntity.badRequest().body(mapOf("reason" to "알 수 없는 역할이다: $role"))

        return respond(userId, parsed, roles.revoke(userId, parsed, actor))
    }

    /**
     * 거절은 409 다. "안 바뀌었다"(200)와 구분되지 않으면 스크립트가 실패를 못 본다.
     *
     * `status` 를 함께 낸다. `changed` 만 보면 **접수됐다**와 **부여됐다**가 둘 다
     * `false`/`true` 한 칸에 뭉개져, 부르는 쪽이 아직 없는 권한을 있다고 믿는다.
     */
    private fun respond(
        userId: String,
        role: AdminRole,
        outcome: RoleOutcome,
    ): ResponseEntity<Map<String, Any>> = when (outcome) {
        is RoleOutcome.Refused ->
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))

        is RoleOutcome.Requested -> ResponseEntity.accepted().body(
            mapOf(
                "userId" to userId,
                "role" to role.name,
                "status" to "REQUESTED",
                "requestId" to outcome.requestId.toString(),
                "changed" to false,
            ),
        )

        else -> ResponseEntity.ok(
            mapOf(
                "userId" to userId,
                "role" to role.name,
                "status" to if (outcome is RoleOutcome.Changed) "GRANTED" else "UNCHANGED",
                "changed" to (outcome is RoleOutcome.Changed),
            ),
        )
    }

    private fun respondTo(
        requestId: UUID,
        outcome: RoleOutcome,
    ): ResponseEntity<Map<String, Any>> = when (outcome) {
        is RoleOutcome.Refused ->
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))

        else -> ResponseEntity.ok(
            mapOf(
                "requestId" to requestId.toString(),
                "changed" to (outcome is RoleOutcome.Changed),
            ),
        )
    }

    @RequiresRole(AdminRole.PUBLISHER)
    @PostMapping("/problems/{problemId}/archive")
    fun archive(
        @PathVariable problemId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ArchiveRequest,
    ): Map<String, String> {
        publish.archive(problemId, actor, request.reason)
        return mapOf("problemId" to problemId, "archived" to "true")
    }

    @GetMapping("/problems/{problemId}")
    fun status(@PathVariable problemId: String): ResponseEntity<ProblemStatus> =
        publish.status(problemId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // --- 재채점 ---

    @RequiresRole(AdminRole.JUDGE_OPERATOR)
    @PostMapping("/rejudges")
    fun requestRejudge(
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: RejudgeRequest,
    ): RejudgeJob = rejudge.request(request.scope, request.reason, actor, request.dryRun)

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/rejudges/{id}/approve")
    fun approveRejudge(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Any> =
        when (val outcome = rejudge.approve(id, actor)) {
            is RejudgeService.ApprovalOutcome.Approved -> ResponseEntity.ok(outcome.job)
            is RejudgeService.ApprovalOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    @RequiresRole(AdminRole.REVIEWER)
    @PostMapping("/rejudges/{id}/reject")
    fun rejectRejudge(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @Valid @RequestBody request: ArchiveRequest,
    ): ResponseEntity<Any> =
        when (val outcome = rejudge.reject(id, actor, request.reason)) {
            is RejudgeService.ApprovalOutcome.Approved -> ResponseEntity.ok(outcome.job)
            is RejudgeService.ApprovalOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    @GetMapping("/rejudges")
    fun listRejudges(@RequestParam(required = false) limit: Int?): List<RejudgeJob> =
        rejudge.recent(limit ?: 20)

    /**
     * 실행. 승인만으로는 아무것도 돌지 않는다 (§11.2).
     *
     * 되돌릴 수 없는 일을 한 번의 클릭으로 시작하지 않기 위해 승인과 실행을 나눈다.
     */
    @RequiresRole(AdminRole.JUDGE_OPERATOR)
    @PostMapping("/rejudges/{id}/dispatch")
    fun dispatchRejudge(
        @PathVariable id: UUID,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Any> =
        when (val outcome = rejudge.dispatch(id, actor)) {
            is RejudgeService.DispatchOutcome.Dispatched ->
                ResponseEntity.accepted().body(
                    mapOf("job" to outcome.job, "targets" to outcome.targets),
                )

            is RejudgeService.DispatchOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    /** 승인된 작업의 대상. 승인 전에는 비어 있다. */
    @GetMapping("/rejudges/{id}/targets")
    fun rejudgeTargets(@PathVariable id: UUID): Map<String, Any> {
        val targets = rejudge.targets(id)
        return mapOf("count" to targets.size, "submissionIds" to targets.map { it.toString() })
    }

    /** 진행 상황과 바뀐 판정. dry-run 이면 "바뀌었을" 판정이다. */
    @GetMapping("/rejudges/{id}")
    fun rejudgeReport(@PathVariable id: UUID): ResponseEntity<RejudgeReport> =
        rejudge.report(id)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // --- 감사 로그 ---

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @GetMapping("/audit")
    fun auditTrail(
        @RequestParam(required = false) subject: String?,
        @RequestParam(required = false) limit: Int?,
    ): List<AuditEntry> = audit.recent(subject, limit ?: 50)
}

data class RegisterVersionRequest(
    val version: Int,
    @field:NotBlank val packageDigest: String,
    @field:NotBlank val reportDigest: String,
    /**
     * 이 보고서를 만든 §6.3 파이프라인의 버전 (§15.3).
     *
     * 필수다. 없으면 digest 가 어긋났을 때 패키지가 바뀐 것인지 파이프라인이 바뀐 것인지
     * 구분할 수 없고, 그 구분이 없으면 공개가 거부됐을 때 무엇을 고쳐야 하는지 알 수 없다.
     */
    @field:NotBlank val validatorVersion: String,
)

data class PublishRequest(
    val version: Int,
    @field:NotBlank val reportDigest: String,
    @field:NotBlank val validatorVersion: String,
)

data class ArchiveRequest(@field:NotBlank val reason: String)

/** 세울 때 검수자가 결함군을 정한다. 기부자는 자기 오답이 무슨 종류인지 모르는 것이 보통이다. */
data class ApproveDonationRequest(@field:NotBlank val kind: String, val note: String? = null)

data class ResolveReportRequest(val retire: Boolean, @field:NotBlank val resolution: String)

data class ResolvePostReportRequest(val hide: Boolean, @field:NotBlank val resolution: String)

data class ResolveFlagRequest(val confirmed: Boolean, val note: String? = null)

data class IssueSanctionRequest(
    @field:NotBlank val userId: String,
    @field:NotBlank val kind: String,
    @field:NotBlank val reason: String,
    @field:NotBlank val evidence: String,
    val days: Int? = null,
)

data class ResolveAppealRequest(val uphold: Boolean, val note: String? = null)

data class CreateContestRequest(
    @field:NotBlank val title: String,
    val kind: String = "CONTEST",
    val problemIds: List<String>,
    val startsAt: java.time.Instant,
    val endsAt: java.time.Instant,
)

/**
 * 역할 부여 요청 (§11.2).
 *
 * 사유를 필수로 받는다. 승인자가 판단할 근거가 없으면 승인은 형식이 되고, 그러면 2인
 * 승인은 사람 수만 채운다 — 재채점이 사유를 요구하는 것과 같은 이유다.
 */
data class RoleRequest(
    @field:NotBlank val role: String,
    @field:NotBlank val reason: String,
) {
    fun parsed(): AdminRole? = runCatching { AdminRole.valueOf(role.uppercase()) }.getOrNull()
}

data class RejudgeRequest(
    @field:NotBlank val scope: String,
    @field:NotBlank val reason: String,
    /** 판정을 바꾸지 않고 무엇이 바뀔지만 본다 (§19.2 FR-721). */
    val dryRun: Boolean = false,
)
