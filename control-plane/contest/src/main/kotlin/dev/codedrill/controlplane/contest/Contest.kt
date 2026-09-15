package dev.codedrill.controlplane.contest

import java.time.Instant
import java.util.UUID

/**
 * 대회 (기획서 §8.4). 비레이팅 연습 대회([Kind.CONTEST])와 미니 대결([Kind.DUEL]).
 *
 * 상태는 저장하지 않고 시각에서 읽는다 — 저장하면 시계와 어긋난다. 대회는 공개 전이면
 * [Status.DRAFT], 시작 전이면 [Status.SCHEDULED], 중이면 [Status.RUNNING], 끝나면
 * [Status.FINISHED]. 대결은 둘째가 붙기 전에는 시작 시각이 없어 [Status.WAITING] 이다.
 *
 * **참가가 곧 이름 공개 동의다.** 순위표는 이름 없이는 뜻이 없다. 참가 시점의 표시 이름을
 * 그 대회의 순위표에만 낸다.
 */
data class Contest(
    val id: UUID,
    val kind: Kind,
    val title: String,
    val createdBy: String,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val minutes: Int?,
    val published: Boolean,
    val joinCode: String?,
    val createdAt: Instant,
) {
    fun status(now: Instant = Instant.now()): Status = when {
        kind == Kind.CONTEST && !published -> Status.DRAFT
        startsAt == null || endsAt == null -> Status.WAITING
        now.isBefore(startsAt) -> Status.SCHEDULED
        now.isBefore(endsAt) -> Status.RUNNING
        else -> Status.FINISHED
    }

    fun running(now: Instant = Instant.now()) = status(now) == Status.RUNNING

    enum class Kind { CONTEST, DUEL }
    enum class Status { DRAFT, WAITING, SCHEDULED, RUNNING, FINISHED }
}

data class Entry(val contestId: UUID, val userId: String, val displayName: String, val joinedAt: Instant)

data class Score(val userId: String, val problemId: String, val bestScore: Int, val attempts: Int, val solvedAt: Instant?)

/** 순위표의 한 줄. 총점이 높은 순, 같으면 마지막 만점이 이른 순. */
data class Standing(
    val rank: Int,
    val displayName: String,
    val mine: Boolean,
    val total: Int,
    val solved: Int,
    val lastSolvedAt: Instant?,
    val perProblem: Map<String, Int>,
)

/** 공개된 문제인가 — 문제 도메인에 묻는다 (§3.1 조립 지점). 대회에는 공개된 문제만 건다. */
fun interface ContestProblems {
    fun published(problemId: String): Boolean

    companion object {
        val NONE = ContestProblems { false }
    }
}
