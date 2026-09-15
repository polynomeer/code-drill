package dev.codedrill.controlplane.workspace

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

/** 문제별 질문 게시판 API (§8.5). */
@RestController
@RequestMapping("/api/v1/discussions")
class DiscussionController(private val service: DiscussionService) {

    @GetMapping("/{problemId}")
    fun questions(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
    ): List<DiscussionService.PostView> = service.questions(principal.id, problemId)

    @PostMapping("/{problemId}")
    fun ask(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable problemId: String,
        @Valid @RequestBody request: AskRequest,
    ): ResponseEntity<Any> =
        posted(service.ask(principal.id, problemId, request.title, request.body, request.anchor?.asRequest(), request.spoiler))

    @GetMapping("/threads/{id}")
    fun thread(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
    ): ResponseEntity<DiscussionService.Thread> {
        val thread = service.thread(principal.id, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(thread)
    }

    @PostMapping("/threads/{id}/answers")
    fun answer(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReplyRequest,
    ): ResponseEntity<Any> = posted(service.answer(principal.id, id, request.body, request.anchor?.asRequest(), request.spoiler))

    @PostMapping("/posts/{id}/reports")
    fun report(
        @RequestAttribute(Principal.ATTRIBUTE) principal: Principal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: PostReportRequest,
    ): ResponseEntity<Any> = when (val outcome = service.report(principal.id, id, request.reason)) {
        is DiscussionService.ReportOutcome.Filed -> ResponseEntity.accepted().body(outcome.report)
        DiscussionService.ReportOutcome.AlreadyFiled -> ResponseEntity.noContent().build()
        is DiscussionService.ReportOutcome.Invalid ->
            ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
    }

    private fun posted(outcome: DiscussionService.PostOutcome): ResponseEntity<Any> = when (outcome) {
        is DiscussionService.PostOutcome.Posted -> ResponseEntity.status(HttpStatus.CREATED).body(outcome.post)
        is DiscussionService.PostOutcome.Invalid -> ResponseEntity.badRequest().body(error(ErrorCode.INVALID_SIGNATURE, outcome.reason))
    }

    private fun error(code: ErrorCode, message: String) = ApiError(code, message, UUID.randomUUID().toString())
}

data class AnchorBody(val submissionId: UUID, val lineFrom: Int? = null, val lineTo: Int? = null, val step: Int? = null) {
    fun asRequest() = DiscussionService.AnchorRequest(submissionId, lineFrom, lineTo, step)
}

data class AskRequest(
    @field:NotBlank val title: String,
    @field:NotBlank val body: String,
    val anchor: AnchorBody? = null,
    val spoiler: Boolean = false,
)

data class ReplyRequest(@field:NotBlank val body: String, val anchor: AnchorBody? = null, val spoiler: Boolean = false)

data class PostReportRequest(@field:NotBlank val reason: String)
