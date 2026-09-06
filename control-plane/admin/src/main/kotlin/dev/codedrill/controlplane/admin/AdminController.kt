package dev.codedrill.controlplane.admin

import jakarta.validation.constraints.NotBlank
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 운영 API (기술 설계서 §3.1 Admin, §9.2).
 *
 * 슬라이스에는 인증이 없다. 행위자는 헤더로 받으며, Identity 모듈과 §11.2 의 관리자 역할
 * (Content Editor / Reviewer / Publisher / Judge Operator / Security Admin)이 붙으면 인증
 * 주체와 역할에서 가져오도록 바꾼다. **지금 이 API 를 공개 환경에 노출하면 안 된다.**
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val publish: PublishService,
    private val rejudge: RejudgeService,
    private val audit: AuditLog,
) {

    // --- 콘텐츠 수명주기 ---

    /** 검증을 통과한 버전을 등록한다 (§6.3 보고서 digest 를 함께 받는다). */
    @PostMapping("/problems/{problemId}/versions")
    fun registerVersion(
        @PathVariable problemId: String,
        @RequestHeader(value = "X-Actor", defaultValue = "unknown") actor: String,
        @RequestBody request: RegisterVersionRequest,
    ): ResponseEntity<Map<String, String>> =
        when (val outcome = publish.registerVersion(
            problemId, request.version, request.packageDigest, request.reportDigest, actor,
        )) {
            is PublishService.RegisterOutcome.Registered ->
                ResponseEntity.ok(mapOf("versionId" to outcome.versionId))

            is PublishService.RegisterOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    /** 공개. 등록자와 다른 사람이어야 한다 (§11.2). */
    @PostMapping("/problems/{problemId}/publish")
    fun publishVersion(
        @PathVariable problemId: String,
        @RequestHeader(value = "X-Actor", defaultValue = "unknown") actor: String,
        @RequestBody request: PublishRequest,
    ): ResponseEntity<Map<String, String>> =
        when (val outcome = publish.publish(problemId, request.version, request.reportDigest, actor)) {
            is PublishService.PublishOutcome.Published ->
                ResponseEntity.ok(mapOf("publishedVersionId" to outcome.versionId))

            is PublishService.PublishOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    @PostMapping("/problems/{problemId}/archive")
    fun archive(
        @PathVariable problemId: String,
        @RequestHeader(value = "X-Actor", defaultValue = "unknown") actor: String,
        @RequestBody request: ArchiveRequest,
    ): Map<String, String> {
        publish.archive(problemId, actor, request.reason)
        return mapOf("problemId" to problemId, "archived" to "true")
    }

    @GetMapping("/problems/{problemId}")
    fun status(@PathVariable problemId: String): ResponseEntity<ProblemStatus> =
        publish.status(problemId)?.let { ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()

    // --- 재채점 ---

    @PostMapping("/rejudges")
    fun requestRejudge(
        @RequestHeader(value = "X-Actor", defaultValue = "unknown") actor: String,
        @RequestBody request: RejudgeRequest,
    ): RejudgeJob = rejudge.request(request.scope, request.reason, actor)

    @PostMapping("/rejudges/{id}/approve")
    fun approveRejudge(
        @PathVariable id: UUID,
        @RequestHeader(value = "X-Actor", defaultValue = "unknown") actor: String,
    ): ResponseEntity<Any> =
        when (val outcome = rejudge.approve(id, actor)) {
            is RejudgeService.ApprovalOutcome.Approved -> ResponseEntity.ok(outcome.job)
            is RejudgeService.ApprovalOutcome.Rejected ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("reason" to outcome.reason))
        }

    @PostMapping("/rejudges/{id}/reject")
    fun rejectRejudge(
        @PathVariable id: UUID,
        @RequestHeader(value = "X-Actor", defaultValue = "unknown") actor: String,
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

    /** 승인된 작업의 대상. 승인 전에는 비어 있다. */
    @GetMapping("/rejudges/{id}/targets")
    fun rejudgeTargets(@PathVariable id: UUID): Map<String, Any> {
        val targets = rejudge.targets(id)
        return mapOf("count" to targets.size, "submissionIds" to targets.map { it.toString() })
    }

    // --- 감사 로그 ---

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
)

data class PublishRequest(val version: Int, @field:NotBlank val reportDigest: String)

data class ArchiveRequest(@field:NotBlank val reason: String)

data class RejudgeRequest(@field:NotBlank val scope: String, @field:NotBlank val reason: String)
