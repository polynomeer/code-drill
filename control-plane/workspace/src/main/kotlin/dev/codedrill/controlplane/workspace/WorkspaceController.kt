package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.common.ApiError
import dev.codedrill.platform.common.ErrorCode
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 초안 API (기술 설계서 §9.2).
 *
 * 슬라이스에는 로그인이 없다. 사용자 식별은 헤더로 받으며, Identity 모듈이 붙으면
 * 인증 주체에서 가져오도록 바꾼다.
 */
@RestController
@RequestMapping("/api/v1/workspaces")
class WorkspaceController(private val service: WorkspaceService) {

    @GetMapping("/{problemId}/{language}")
    fun get(
        @PathVariable problemId: String,
        @PathVariable language: String,
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
    ): ResponseEntity<DraftResponse> {
        val draft = service.find(userId, problemId, language.uppercase())
            ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(DraftResponse.of(draft))
    }

    /** 최근에 손댄 초안. "이어서 풀기" 목록이 된다. */
    @GetMapping
    fun recent(
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
    ): List<DraftResponse> = service.recent(userId).map(DraftResponse::of)

    /**
     * 초안 저장 (CAS).
     *
     * 충돌이면 409 와 함께 서버의 현재 초안을 실어 보낸다. 클라이언트는 그것으로 사용자에게
     * "내 것 유지 / 서버 것 가져오기"를 물을 수 있다 (§9.4 Conflict → 새 상태 확인).
     */
    @PutMapping("/{problemId}/{language}")
    fun save(
        @PathVariable problemId: String,
        @PathVariable language: String,
        @RequestHeader(value = "X-User-Id", defaultValue = "demo-user") userId: String,
        @RequestBody request: SaveDraftRequest,
    ): ResponseEntity<Any> =
        when (val outcome = service.save(userId, problemId, language.uppercase(), request.code, request.version)) {
            is WorkspaceService.SaveOutcome.Saved ->
                ResponseEntity.ok(SaveDraftResponse(outcome.version, Instant.now()))

            is WorkspaceService.SaveOutcome.Conflict ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(
                    DraftConflict(
                        error = ApiError(
                            ErrorCode.DRAFT_VERSION_CONFLICT,
                            "그 사이에 다른 곳에서 초안이 저장됐다",
                            UUID.randomUUID().toString(),
                        ),
                        current = DraftResponse.of(outcome.current),
                    ),
                )
        }
}

data class SaveDraftRequest(
    @field:NotBlank val code: String,
    /** 마지막으로 본 버전. 초안이 없다고 믿으면 null 이다. */
    val version: Long?,
)

data class SaveDraftResponse(val version: Long, val syncedAt: Instant)

data class DraftResponse(
    val problemId: String,
    val language: String,
    val code: String,
    val version: Long,
    val updatedAt: Instant,
) {
    companion object {
        fun of(draft: WorkspaceDraft) = DraftResponse(
            problemId = draft.problemId,
            language = draft.language,
            code = draft.code,
            version = draft.version,
            updatedAt = draft.updatedAt,
        )
    }
}

data class DraftConflict(val error: ApiError, val current: DraftResponse)
