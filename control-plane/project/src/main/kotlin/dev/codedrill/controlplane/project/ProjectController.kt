package dev.codedrill.controlplane.project

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import dev.codedrill.platform.common.Principal
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
