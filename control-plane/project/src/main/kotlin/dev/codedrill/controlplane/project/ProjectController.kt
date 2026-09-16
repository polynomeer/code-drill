package dev.codedrill.controlplane.project

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 프로젝트형 문제 API (feature-roadmap 11단계).
 *
 * 목록·상세는 문제 목록처럼 로그인 없이 열리고, 제출부터는 사용자의 것이다. **남의 제출은
 * 없는 것처럼 보인다** — 404 다 (§11.4).
 */
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(private val service: ProjectService) {

    @GetMapping
    fun list(@RequestAttribute(name = Principal.ATTRIBUTE, required = false) principal: Principal?) =
        service.list(principal?.id)

    /** 내 제출 기록. `/{id}` 보다 먼저 있어야 `submissions` 가 프로젝트 id 로 읽히지 않는다. */
    @GetMapping("/submissions")
    fun history(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @RequestParam(required = false) projectId: String?,
        @RequestParam(required = false) limit: Int?,
    ): List<ProjectSubmissionResponse> = service.history(principal.id, projectId, limit).map { ProjectSubmissionResponse.of(it) }

    /** 제출 하나. 소유자에게만, 파일까지. */
    @GetMapping("/submissions/{id}")
    fun submission(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<ProjectSubmissionResponse> {
        val submission = service.find(id)?.takeIf { it.userId == principal.id } ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ProjectSubmissionResponse.of(submission, service.files(id)))
    }

    // --- 초안 (§8.1). 파일 여럿의 자동 저장, CAS. ---

    @GetMapping("/{id}/draft")
    fun draft(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: String,
    ): ResponseEntity<ProjectDraftResponse> {
        val draft = service.draft(principal.id, id) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(ProjectDraftResponse.of(draft))
    }

    /** 충돌이면 409 와 함께 서버의 현재 초안을 실어 보낸다 — "내 것 유지 / 서버 것 가져오기"를 물을 수 있게. */
    @PutMapping("/{id}/draft")
    fun saveDraft(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: String,
        @Valid @RequestBody request: SaveProjectDraftRequest,
    ): ResponseEntity<Any> = when (val outcome = service.saveDraft(principal.id, id, request.files, request.version)) {
        is ProjectService.DraftOutcome.Saved -> ResponseEntity.ok(mapOf("version" to outcome.version, "syncedAt" to java.time.Instant.now()))
        is ProjectService.DraftOutcome.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT).body(
            mapOf("error" to error(ErrorCode.DRAFT_VERSION_CONFLICT, "그 사이에 다른 곳에서 초안이 저장됐다"), "current" to ProjectDraftResponse.of(outcome.current)),
        )
        is ProjectService.DraftOutcome.Invalid -> ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
    }

    /** 초안을 버린다 — "시작 저장소로 되돌리기". */
    @DeleteMapping("/{id}/draft")
    fun discardDraft(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: String,
    ): ResponseEntity<Void> {
        service.discardDraft(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    /** 판정 이력 (§8.1). 재채점이 무엇을 무엇으로 바꿨는지 사용자도 본다. */
    @GetMapping("/submissions/{id}/judgements")
    fun judgements(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<List<ProjectJudgement>> {
        service.find(id)?.takeIf { it.userId == principal.id } ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(service.judgements(id))
    }

    @GetMapping("/{id}")
    fun view(@PathVariable id: String): ResponseEntity<ProjectService.ProjectView> {
        val view = service.view(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(view)
    }

    @PostMapping("/{id}/submissions")
    fun submit(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: String,
        @Valid @RequestBody request: SubmitProjectRequest,
    ): ResponseEntity<Any> = when (val outcome = service.submit(principal.id, idempotencyKey, id, request.files)) {
        is ProjectService.SubmitOutcome.Accepted ->
            ResponseEntity.status(HttpStatus.ACCEPTED).body(ProjectSubmissionResponse.of(outcome.submission))
        is ProjectService.SubmitOutcome.Invalid ->
            ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
        is ProjectService.SubmitOutcome.Throttled ->
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error(ErrorCode.QUOTA_EXCEEDED, outcome.reason))
        ProjectService.SubmitOutcome.NotFound -> ResponseEntity.notFound().build()
    }

    private fun error(code: ErrorCode, message: String) = ApiError(code, message, UUID.randomUUID().toString())
}

/** 제출 본문. 경로 → 내용. 한계는 [dev.codedrill.judge.protocol.Workspaces] 가 정한다. */
data class SubmitProjectRequest(@field:NotEmpty val files: Map<String, String>)

data class SaveProjectDraftRequest(@field:NotEmpty val files: Map<String, String>, val version: Long?)

data class ProjectDraftResponse(val projectId: String, val files: Map<String, String>, val version: Long, val updatedAt: java.time.Instant) {
    companion object {
        fun of(draft: ProjectDraft) = ProjectDraftResponse(draft.projectId, draft.files, draft.version, draft.updatedAt)
    }
}
