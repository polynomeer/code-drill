package dev.codedrill.controlplane.admin

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
 * 행위자는 [AdminAuthInterceptor] 가 토큰에서 확인한 이름이다. 컨트롤러는 자칭한 값을
 * 받지 않으며, 감사 로그의 actor 도 이 이름이다.
 *
 * 아직 사용자 세션과 통합돼 있지 않다. 토큰은 설정에 든 장기 비밀이므로, Identity
 * 모듈이 붙으면 워크로드 ID 와 짧은 수명 토큰으로 옮긴다 (§11.2).
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val publish: PublishService,
    private val rejudge: RejudgeService,
    private val audit: AuditLog,
    private val roles: AdminRoles,
) {

    // --- 콘텐츠 수명주기 ---

    /** 검증을 통과한 버전을 등록한다 (§6.3 보고서 digest 를 함께 받는다). */
    @RequiresRole(AdminRole.CONTENT_EDITOR)
    @PostMapping("/problems/{problemId}/versions")
    fun registerVersion(
        @PathVariable problemId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @RequestBody request: RegisterVersionRequest,
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
        @RequestBody request: PublishRequest,
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

    /** 역할을 준다. 부여 자체가 감사 대상이다 (§13.3). */
    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @PostMapping("/operators/{userId}/roles")
    fun grantRole(
        @PathVariable userId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @RequestBody request: RoleRequest,
    ): ResponseEntity<Map<String, Any>> {
        val role = request.parsed()
            ?: return ResponseEntity.badRequest().body(mapOf("reason" to "알 수 없는 역할이다: ${request.role}"))

        return ResponseEntity.ok(
            mapOf("userId" to userId, "role" to role.name, "changed" to roles.grant(userId, role, actor)),
        )
    }

    @RequiresRole(AdminRole.SECURITY_ADMIN)
    @DeleteMapping("/operators/{userId}/roles/{role}")
    fun revokeRole(
        @PathVariable userId: String,
        @PathVariable role: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
    ): ResponseEntity<Map<String, Any>> {
        val parsed = runCatching { AdminRole.valueOf(role.uppercase()) }.getOrNull()
            ?: return ResponseEntity.badRequest().body(mapOf("reason" to "알 수 없는 역할이다: $role"))

        return ResponseEntity.ok(
            mapOf("userId" to userId, "role" to parsed.name, "changed" to roles.revoke(userId, parsed, actor)),
        )
    }

    @RequiresRole(AdminRole.PUBLISHER)
    @PostMapping("/problems/{problemId}/archive")
    fun archive(
        @PathVariable problemId: String,
        @RequestAttribute(AdminAuthInterceptor.ACTOR_ATTRIBUTE) actor: String,
        @RequestBody request: ArchiveRequest,
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
        @RequestBody request: RejudgeRequest,
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
        @RequestBody request: ArchiveRequest,
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

data class RoleRequest(@field:NotBlank val role: String) {
    fun parsed(): AdminRole? = runCatching { AdminRole.valueOf(role.uppercase()) }.getOrNull()
}

data class RejudgeRequest(
    @field:NotBlank val scope: String,
    @field:NotBlank val reason: String,
    /** 판정을 바꾸지 않고 무엇이 바뀔지만 본다 (§19.2 FR-721). */
    val dryRun: Boolean = false,
)
