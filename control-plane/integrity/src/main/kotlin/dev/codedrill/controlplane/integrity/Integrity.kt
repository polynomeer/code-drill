package dev.codedrill.controlplane.integrity

import java.time.Instant
import java.util.UUID

/**
 * 유사도 신호 (§11.4 부정행위 방어, §10.4 정책).
 *
 * 맞힌 제출 하나가 같은 문제·같은 언어의 **다른 사람** 제출과 구조가 겹칠 때 한 줄이
 * 선다. 두 제출은 순서가 없다 — 같은 쌍이 두 번 서지 않도록 작은 id 가 앞이다.
 *
 * 신호는 판정을 바꾸지 않는다. 검수자가 두 소스를 나란히 보고 확인하거나 기각하며,
 * 확인된 것도 지금은 기록일 뿐이다 — 제재는 대회와 함께 온다 (§8.5 단계적 제재).
 */
data class SimilarityFlag(
    val id: UUID,
    val problemId: String,
    val language: String,
    val submissionId: UUID,
    val otherSubmissionId: UUID,
    /** 계정을 지우면 null. 신호는 남되 누구의 것인지는 남지 않는다 (§11.3). */
    val userId: String?,
    val otherUserId: String?,
    val score: Double,
    val status: FlagStatus,
    val reviewedBy: String?,
    val reviewedAt: Instant?,
    val note: String?,
    val createdAt: Instant,
)

enum class FlagStatus { OPEN, CONFIRMED, DISMISSED }

/** 저장된 지문. 비교에 필요한 것만 — 소스는 여기 없다. */
data class StoredPrint(
    val submissionId: UUID,
    val userId: String?,
    val hashes: Set<Int>,
)

/**
 * 검수자가 두 소스를 나란히 보기 위한 창구 (§3.1 조립 지점). 이 모듈은 소스를 갖지 않는다 —
 * 지문만 갖고, 소스는 볼 때만 제출 도메인에 묻는다.
 */
fun interface SubmissionSources {
    fun source(submissionId: UUID): String?

    companion object {
        val NONE = SubmissionSources { null }
    }
}
