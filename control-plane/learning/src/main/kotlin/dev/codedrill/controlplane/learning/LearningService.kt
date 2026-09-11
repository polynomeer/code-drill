package dev.codedrill.controlplane.learning

import dev.codedrill.platform.common.LearningRhythm
import dev.codedrill.platform.problempackage.ProblemCatalog
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * 학습 루프 (PRD FR-808, FR-205, 기획서 §8.1).
 *
 * 처방·리포트·통계는 **저장하지 않고 계산한다.** 숙련도를 저장하지 않는 것과 같은 이유다 —
 * 규칙을 고쳤을 때 옛 값이 새 규칙과 섞이면 안 되고, "이 추천이 어디서 나왔나"에 답하려면
 * 결국 사실로 돌아가야 한다. 저장하는 것은 사용자가 한 일뿐이다: 밀어낸 문제, 만든 문제집.
 */
@Service
class LearningService(
    private val repository: LearningRepository,
    private val packages: ProblemPackageLoader,
    private val sources: LearningSources = LearningSources.NONE,
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {

    fun prescription(userId: String, now: Instant = Instant.now()): Prescription =
        Prescriber.prescribe(facts(userId, now), now, zone)

    /** 오늘의 처방에서 문제를 밀어낸다 (FR-808). 내일이면 다시 권할 수 있다. */
    @Transactional
    fun skip(userId: String, problemId: String, now: Instant = Instant.now()): Prescription {
        repository.skip(userId, problemId, LocalDate.ofInstant(now, zone))
        return prescription(userId, now)
    }

    fun weekly(userId: String, now: Instant = Instant.now()): WeeklyReport {
        val weekAgo = now.minus(WEEK)
        val facts = facts(userId, now)
        val thisWeek = facts.attempts.filter { it.at >= weekAgo }

        val before = facts.standing
        val past = sources.standing(userId, weekAgo)
        // 성장 = 재고 있던 역량이 올라간 것. UNMEASURED → DEVELOPING 은 성장이 아니라
        // 첫 측정이다 — 처음 틀린 것을 "올랐다"고 적으면 리포트가 거짓말을 한다.
        val growth = before.mapNotNull { (competency, standing) ->
            val was = past[competency] ?: Standing.UNMEASURED
            if (was != Standing.UNMEASURED && standing.ordinal > was.ordinal) Growth(competency, was, standing) else null
        }.sortedBy { it.competency }

        // 재발: 이번 주에 틀린 문제 중, 그 전에 맞힌 적이 있는 것.
        val solvedBefore = facts.attempts.filter { it.accepted && it.at < weekAgo }
            .groupBy { it.problemId }.mapValues { (_, list) -> list.maxBy { it.at }.at }
        val recurrences = thisWeek.filter { !it.accepted && it.problemId in solvedBefore }
            .groupBy { it.problemId }
            .map { (id, fails) -> Recurrence(id, solvedBefore.getValue(id), fails.maxBy { it.at }.at) }
            .sortedByDescending { it.failedAt }

        val prescription = Prescriber.prescribe(facts, now, zone)

        return WeeklyReport(
            from = LocalDate.ofInstant(weekAgo, zone),
            to = LocalDate.ofInstant(now, zone),
            activity = Activity(
                attempts = thisWeek.size,
                accepted = thisWeek.count { it.accepted },
                problemsSolved = thisWeek.filter { it.accepted }.map { it.problemId }.distinct().size,
                activeDays = thisWeek.map { LocalDate.ofInstant(it.at, zone) }.distinct().size,
            ),
            weakest = before.filterValues { it == Standing.DEVELOPING }.keys.sorted(),
            recurrences = recurrences,
            growth = growth,
            actions = prescription.items,
            nextMeasurement = prescription.items.minOfOrNull { it.nextMeasurement }
                ?: now.plus(LearningRhythm.REVIEW_INTERVAL),
        )
    }

    fun stats(userId: String, now: Instant = Instant.now()): Stats {
        val all = sources.attempts(userId, Instant.EPOCH)
        val recent = all.filter { it.at >= now.minus(STATS_WINDOW) }
        val catalogs = catalogs()
        val solved = sources.solved(userId)
        val attempted = all.map { it.problemId }.toSet()

        val byTag = catalogs.entries
            .flatMap { (id, catalog) -> catalog.tags.map { tag -> tag to id } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, ids) ->
                TagStat(attempted = ids.count { it in attempted }, solved = ids.count { it in solved })
            }
            .filterValues { it.attempted > 0 }

        return Stats(
            problemsAttempted = attempted.size,
            problemsSolved = solved.size,
            submissions = all.size,
            accepted = all.count { it.accepted },
            verdicts = recent.groupingBy { it.verdict }.eachCount(),
            byTag = byTag,
        )
    }

    // --- 문제집 (FR-205) ---

    fun collections(userId: String): List<Collection> = repository.collections(userId)

    @Transactional
    fun createCollection(userId: String, name: String): Collection {
        val id = UUID.randomUUID()
        repository.createCollection(id, userId, name.trim())
        return Collection(id, name.trim(), emptyList())
    }

    @Transactional
    fun addToCollection(userId: String, collectionId: UUID, problemId: String): Boolean {
        if (!repository.owns(userId, collectionId)) return false
        if (problemId !in sources.published()) return false
        repository.addItem(collectionId, problemId)
        return true
    }

    @Transactional
    fun removeFromCollection(userId: String, collectionId: UUID, problemId: String): Boolean {
        if (!repository.owns(userId, collectionId)) return false
        repository.removeItem(collectionId, problemId)
        return true
    }

    @Transactional
    fun deleteCollection(userId: String, collectionId: UUID): Boolean =
        repository.owns(userId, collectionId) && repository.deleteCollection(collectionId) > 0

    private fun facts(userId: String, now: Instant) = Prescriber.Facts(
        attempts = sources.attempts(userId, now.minus(HISTORY)),
        solved = sources.solved(userId),
        helpLevel = { sources.helpLevel(userId, it) },
        pendingTransfer = sources.pendingTransfer(userId),
        standing = sources.standing(userId, now),
        catalogs = catalogs(),
        skipped = repository.skipped(userId, LocalDate.ofInstant(now, zone)),
    )

    /** 공개된 문제의 카탈로그. 읽지 못하는 문제는 권하지 않는다. */
    private fun catalogs(): Map<String, ProblemCatalog> =
        sources.published().mapNotNull { id ->
            runCatching { packages.load(id).catalog }.getOrNull()?.let { id to it }
        }.toMap()

    private companion object {
        val WEEK: Duration = Duration.ofDays(7)
        val STATS_WINDOW: Duration = Duration.ofDays(30)

        /**
         * 처방이 되돌아보는 기간.
         *
         * 이보다 오래된 시도는 복습 대상에서도 빠진다 — 석 달 전에 맞힌 문제를 "복습 시점"
         * 이라 권하면 목록이 옛 문제로만 채워지고, 지금 배우는 것과 멀어진다.
         */
        val HISTORY: Duration = Duration.ofDays(90)
    }
}
