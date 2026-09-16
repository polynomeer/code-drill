package dev.codedrill.controlplane.contest

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 대회와 미니 대결 (§8.4).
 *
 * 대회는 운영자가 만들고 공개하며 누구나 참가한다. 대결은 사용자가 문제 하나로 만들고,
 * 코드를 받은 한 사람이 붙는 순간 시작한다 — 짧은 문제를 동시에 풀고 끝난 뒤 순위표를 본다.
 *
 * 점수는 판정에서 온다. 제출 도메인이 판정을 확정하면 조립 지점이 [judged] 를 부르고, 이
 * 서비스는 "이 사람이 참가 중이고 지금 돌고 있으며 이 문제를 건 대회"에만 점수를 적는다.
 * 대회 밖의 제출은 대회와 무관하다 — 문제는 그대로 문제다.
 */
@Service
class ContestService(
    private val repository: ContestRepository,
    private val problems: ContestProblems = ContestProblems.NONE,
) {

    // --- 읽기 -------------------------------------------------------------------

    fun list(userId: String): List<ContestSummary> =
        repository.visible(userId, LIST_LIMIT).map { it.summary(repository.entry(it.id, userId) != null, repository.entryCount(it.id)) }

    fun view(userId: String, id: UUID): ContestView? {
        val contest = repository.find(id) ?: return null
        val entry = repository.entry(id, userId)
        // 공개 전 대회는 없는 것이다. 대결은 코드를 아는 사람에게만 열리므로, 참가하지 않았으면 없는 것이다.
        if (contest.kind != Contest.Kind.DUEL && !contest.published) return null
        if (contest.kind == Contest.Kind.DUEL && entry == null) return null
        return ContestView(
            contest = contest.summary(entry != null, repository.entryCount(id)),
            problems = repository.problems(id),
            standings = standings(userId, contest),
            // 코드는 만든 사람에게만 — 남에게 알리는 것은 그 사람의 몫이다.
            joinCode = contest.joinCode?.takeIf { contest.createdBy == userId },
        )
    }

    /** 순위표. 총점 높은 순, 같으면 마지막 만점이 이른 순. 참가했지만 점수가 없는 사람도 줄에 있다. */
    fun standings(readerId: String, contest: Contest): List<Standing> {
        val entries = repository.entries(contest.id)
        val byUser = repository.scores(contest.id).groupBy { it.userId }
        val rows = entries.map { entry ->
            val scores = byUser[entry.userId].orEmpty()
            val total = scores.sumOf { it.bestScore }
            Standing(
                rank = 0, displayName = entry.displayName, mine = entry.userId == readerId, total = total,
                solved = scores.count { it.solvedAt != null }, lastSolvedAt = scores.mapNotNull { it.solvedAt }.maxOrNull(),
                perProblem = scores.associate { it.problemId to it.bestScore },
            )
        }.sortedWith(compareByDescending<Standing> { it.total }.thenBy { it.lastSolvedAt ?: Instant.MAX }.thenBy { it.displayName })
        return rows.mapIndexed { i, row -> row.copy(rank = i + 1) }
    }

    // --- 참가와 대결 ----------------------------------------------------------------

    /**
     * 참가한다. 참가가 곧 이름 공개 동의다 — 그때의 표시 이름이 이 대회의 순위표에 오른다.
     * 끝난 대회에는 참가하지 못한다.
     */
    @Transactional
    fun join(userId: String, displayName: String, id: UUID): JoinOutcome {
        val contest = repository.find(id)?.takeIf { it.kind != Contest.Kind.DUEL && it.published } ?: return JoinOutcome.Invalid("그런 대회가 없다")
        if (contest.status() == Contest.Status.FINISHED) return JoinOutcome.Invalid("끝난 대회다")
        return if (repository.join(id, userId, displayName)) JoinOutcome.Joined(contest.summary(true, repository.entryCount(id))) else JoinOutcome.AlreadyJoined
    }

    /** 대결을 연다. 문제 하나, 몇 분. 코드를 받아 상대에게 알린다. */
    @Transactional
    fun openDuel(userId: String, displayName: String, problemId: String, minutes: Int): DuelOutcome {
        if (!problems.published(problemId)) return DuelOutcome.Invalid("공개된 문제가 아니다")
        if (minutes !in MIN_DUEL_MINUTES..MAX_DUEL_MINUTES) return DuelOutcome.Invalid("대결은 ${MIN_DUEL_MINUTES}~${MAX_DUEL_MINUTES}분")
        val duel = Contest(
            id = UUID.randomUUID(), kind = Contest.Kind.DUEL, title = "미니 대결: $problemId", createdBy = userId,
            startsAt = null, endsAt = null, minutes = minutes, published = true, joinCode = code(), createdAt = Instant.now(),
        )
        repository.insert(duel, listOf(problemId))
        repository.join(duel.id, userId, displayName)
        return DuelOutcome.Opened(duel.summary(true, 1), duel.joinCode!!)
    }

    /** 코드로 붙는다. 둘째가 붙는 순간 시작한다. 셋째는 없다 — 대결은 둘이다. */
    @Transactional
    fun joinDuel(userId: String, displayName: String, code: String): DuelOutcome {
        val duel = repository.findByCode(code.trim().uppercase())?.takeIf { it.kind == Contest.Kind.DUEL } ?: return DuelOutcome.Invalid("그런 코드가 없다")
        if (repository.entry(duel.id, userId) != null) return DuelOutcome.Invalid("이미 이 대결에 있다")
        if (duel.startsAt != null) return DuelOutcome.Invalid("이미 시작한 대결이다")
        repository.join(duel.id, userId, displayName)
        val now = Instant.now()
        repository.start(duel.id, now, now.plus(Duration.ofMinutes(duel.minutes!!.toLong())))
        return DuelOutcome.Opened(repository.find(duel.id)!!.summary(true, 2), null)
    }

    // --- 운영자 -----------------------------------------------------------------

    fun create(createdBy: String, kind: Contest.Kind, title: String, problemIds: List<String>, startsAt: Instant, endsAt: Instant): AdminOutcome {
        if (kind == Contest.Kind.DUEL) return AdminOutcome.Rejected("대결은 사용자가 연다")
        if (title.isBlank()) return AdminOutcome.Rejected("제목이 필요하다")
        if (problemIds.isEmpty() || problemIds.size > MAX_PROBLEMS) return AdminOutcome.Rejected("문제는 1~${MAX_PROBLEMS}개")
        if (problemIds.toSet().size != problemIds.size) return AdminOutcome.Rejected("같은 문제가 두 번 있다")
        problemIds.firstOrNull { !problems.published(it) }?.let { return AdminOutcome.Rejected("공개된 문제가 아니다: $it") }
        if (!endsAt.isAfter(startsAt)) return AdminOutcome.Rejected("끝이 시작보다 뒤여야 한다")
        val contest = Contest(
            id = UUID.randomUUID(), kind = kind, title = title.trim(), createdBy = createdBy,
            startsAt = startsAt, endsAt = endsAt, minutes = null, published = false, joinCode = null, createdAt = Instant.now(),
        )
        repository.insert(contest, problemIds)
        return AdminOutcome.Decided(contest)
    }

    /** 연다. 만든 사람은 못 연다 — 문제 공개와 같은 2인 원칙이다 (§11.2). */
    fun publish(id: UUID, actor: String): AdminOutcome {
        val contest = repository.find(id)?.takeIf { it.kind != Contest.Kind.DUEL } ?: return AdminOutcome.Rejected("그런 대회가 없다")
        if (contest.createdBy == actor) return AdminOutcome.Rejected("만든 사람은 열지 못한다 — 다른 사람이 연다 (2인 승인)")
        if (repository.publish(id) == 0) return AdminOutcome.Rejected("이미 공개된 대회다")
        return AdminOutcome.Decided(repository.find(id)!!)
    }

    // --- 판정 -------------------------------------------------------------------

    /** 판정이 확정됐다. 참가 중이고 돌고 있으며 이 문제를 건 대회에만 점수를 적는다. */
    @Transactional
    fun judged(userId: String, problemId: String, score: Int, full: Boolean, submittedAt: Instant) {
        for (contest in repository.runningFor(userId, problemId, submittedAt, SOLVING)) {
            repository.score(contest.id, userId, problemId, score, full, submittedAt)
        }
    }

    /** 반례 대전 중에 과녁을 깨뜨렸다 (§8.4). 서로 다른 과녁의 수가 점수다. */
    @Transactional
    fun hacked(userId: String, problemId: String, targets: List<String>, at: Instant) {
        if (targets.isEmpty()) return
        for (contest in repository.runningFor(userId, problemId, at, setOf(Contest.Kind.HACK))) {
            val fresh = targets.count { repository.hack(contest.id, userId, problemId, it, at) }
            if (fresh > 0) repository.scoreHack(contest.id, userId, problemId, repository.hackCount(contest.id, userId, problemId), at)
        }
    }

    private fun Contest.summary(joined: Boolean, entrants: Int) = ContestSummary(
        id = id, kind = kind, title = title, status = status(), startsAt = startsAt, endsAt = endsAt, minutes = minutes,
        joined = joined, entrants = entrants, problemCount = repository.problems(id).size,
    )

    private fun code(): String = buildString { repeat(CODE_LENGTH) { append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]) } }

    data class ContestSummary(
        val id: UUID, val kind: Contest.Kind, val title: String, val status: Contest.Status,
        val startsAt: Instant?, val endsAt: Instant?, val minutes: Int?,
        val joined: Boolean, val entrants: Int, val problemCount: Int,
    )

    data class ContestView(val contest: ContestSummary, val problems: List<String>, val standings: List<Standing>, val joinCode: String?)

    sealed interface JoinOutcome {
        data class Joined(val contest: ContestSummary) : JoinOutcome
        data object AlreadyJoined : JoinOutcome
        data class Invalid(val reason: String) : JoinOutcome
    }

    sealed interface DuelOutcome {
        data class Opened(val contest: ContestSummary, val joinCode: String?) : DuelOutcome
        data class Invalid(val reason: String) : DuelOutcome
    }

    sealed interface AdminOutcome {
        data class Decided(val contest: Contest) : AdminOutcome
        data class Rejected(val reason: String) : AdminOutcome
    }

    companion object {
        /** 판정이 점수인 종류. 반례 대전은 판정이 아니라 깨뜨린 과녁이 점수다. */
        val SOLVING = setOf(Contest.Kind.CONTEST, Contest.Kind.DUEL)
        const val LIST_LIMIT = 50
        const val MAX_PROBLEMS = 10
        const val MIN_DUEL_MINUTES = 5
        const val MAX_DUEL_MINUTES = 120
        const val CODE_LENGTH = 6
        /** 헷갈리는 글자(0/O, 1/I)를 뺀 알파벳. */
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val RANDOM = SecureRandom()
    }
}
