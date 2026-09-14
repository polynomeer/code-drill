package dev.codedrill.controlplane.workspace

import dev.codedrill.platform.problempackage.DefectKind
import java.time.Instant
import java.util.UUID

/**
 * 아레나에 세운 남의 오답 (기획서 §8.3 "익명화된 오답", §8.5 신고·검수).
 *
 * 저작자의 대표 오답과 나란히 과녁이 된다. 다른 것은 **출처**다 — 사용자의 실제 제출이고,
 * 그래서 세 단계를 거친다.
 *
 * - **기부**: 본인만 내놓을 수 있다. 익명화해도 코드는 그 사람의 것이다.
 * - **검수**: 검수자가 보고 세운다 (§8.3 마지막 줄 — 악의적 데이터와 정답 노출 방지).
 *   실행은 어차피 샌드박스라 위험한 것은 코드가 아니라 **내용**이다: 정답을 살짝 바꾼
 *   것처럼 보이지만 실은 정답이거나, 다른 사람의 풀이를 베낀 것이거나.
 * - **신고**: 세워진 뒤 맞힌 사람이 문제를 발견하면 올리고, 검수자가 내린다.
 *
 * 기부자의 이름은 어디에도 나가지 않는다. 과녁의 이름은 기부의 id 에서 만든다.
 */
data class ArenaDonation(
    val id: UUID,
    val problemId: String,
    val submissionId: UUID,
    val donorUserId: String,
    val source: String?,
    val note: String,
    val status: DonationStatus,
    val kind: DefectKind?,
    val reviewedBy: String?,
    val reviewedAt: Instant?,
    val reason: String?,
    val createdAt: Instant,
) {
    /** 과녁으로서의 이름. 기록판이 이 이름으로 센다. */
    val targetName: String get() = targetName(id)

    companion object {
        const val PREFIX = "community-"
        fun targetName(id: UUID) = PREFIX + id.toString().take(8)
    }
}

enum class DonationStatus { PENDING, APPROVED, REJECTED, RETIRED }

data class ArenaReport(
    val id: UUID,
    val donationId: UUID,
    val reporterId: String,
    val reason: String,
    val status: ReportStatus,
    val resolvedBy: String?,
    val resolvedAt: Instant?,
    val resolution: String?,
    val createdAt: Instant,
)

enum class ReportStatus { OPEN, RETIRED, DISMISSED }

/**
 * 기부할 수 있는 제출을 제출 도메인에 묻는다 (§3.1 조립 지점).
 *
 * 워크스페이스는 제출 표를 읽지 않는다. 무엇이 "틀린 제출"인지는 판정의 것이다.
 */
interface DonatableSubmissions {

    /** 이 사람의 것이고, 이 문제이고, 판정이 오답이고, 아레나가 돌릴 수 있는 언어인 제출. */
    fun donatable(userId: String, problemId: String): List<DonatableSubmission>

    /** 그 제출 하나. 조건에 맞지 않으면 null. */
    fun donatable(userId: String, submissionId: UUID): DonatableSubmission?

    companion object {
        val NONE = object : DonatableSubmissions {
            override fun donatable(userId: String, problemId: String) = emptyList<DonatableSubmission>()
            override fun donatable(userId: String, submissionId: UUID): DonatableSubmission? = null
        }
    }
}

data class DonatableSubmission(
    val id: UUID,
    val problemId: String,
    val source: String,
    val score: Int?,
    val createdAt: Instant,
)
