package dev.codedrill.controlplane.workspace

import java.time.Instant

/**
 * 작업 중인 초안 (기술 설계서 §8.1).
 *
 * [version] 은 저장할 때마다 1 씩 오른다. 클라이언트는 자기가 마지막으로 본 버전을
 * 함께 보내고, 서버는 그 버전일 때만 덮어쓴다 (§8.2 version for CAS).
 */
data class WorkspaceDraft(
    val userId: String,
    val problemId: String,
    val language: String,
    val code: String,
    val version: Long,
    val updatedAt: Instant,
)
