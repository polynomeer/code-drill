package dev.codedrill.controlplane.learning

import dev.codedrill.platform.common.LearningRhythm
import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.ProblemCatalog
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 처방을 만든다 (PRD FR-808, 기획서 §8.1).
 *
 * > 정답률보다 최근 오답 원인, 힌트 의존도, 복습 간격과 전이 성과를 추천에 반영합니다.
 *
 * 이유마다 후보를 하나씩 고르고, [Reason] 의 순서대로 자리를 채운다. 정답률은 어디에도
 * 없다 — 정답률이 높은 문제를 권하면 쉬운 것만 돌게 되고, 낮은 문제를 권하면 어려운 것에
 * 부딪히기만 한다. 둘 다 "이 사람에게 지금 무엇이 필요한가"와 무관하다.
 *
 * **결정적이다.** 같은 상태에서는 같은 처방이 나온다. 무작위로 고르면 "왜 이 문제인가"에
 * 답할 수 없고, 사용자는 새로고침으로 마음에 드는 문제를 뽑게 된다.
 *
 * 순수 함수다. 시각과 사실을 받아 처방을 낸다 — 그래서 시험할 수 있다.
 */
object Prescriber {

    /** 하루에 몇 문제까지. 셋을 넘기면 처방이 아니라 숙제다. */
    const val DAILY_LIMIT = 3

    /** "최근"의 기준. 이 안에 틀린 문제는 원인이 아직 손에 있다. */
    val RECENT: Duration = Duration.ofDays(7)

    data class Facts(
        val attempts: List<Attempt>,
        val solved: Set<String>,
        val helpLevel: (String) -> Int,
        val pendingTransfer: String?,
        val standing: Map<Competency, Standing>,
        val catalogs: Map<String, ProblemCatalog>,
        /** 오늘 사용자가 밀어낸 문제. 조정할 수 있어야 처방이다 (FR-808). */
        val skipped: Set<String>,
    )

    fun prescribe(facts: Facts, now: Instant, zone: ZoneId): Prescription {
        val picked = linkedMapOf<String, PrescribedProblem>()
        val taken = { id: String -> id in picked || id in facts.skipped }

        fun offer(candidate: PrescribedProblem?) {
            if (candidate == null || picked.size >= DAILY_LIMIT || taken(candidate.problemId)) return
            if (candidate.problemId !in facts.catalogs) return
            picked[candidate.problemId] = candidate
        }

        offer(recentFailure(facts, now))
        offer(transfer(facts, now))
        offer(reviewDue(facts, now))
        offer(weakCompetency(facts, now, taken))
        offer(nextOnPath(facts, now, taken))

        return Prescription(
            date = LocalDate.ofInstant(now, zone),
            items = picked.values.toList(),
            streak = streakOf(facts.attempts, now, zone),
        )
    }

    /** 최근에 틀렸고 그 뒤로 맞히지 못한 문제 중 가장 최근 것. */
    private fun recentFailure(facts: Facts, now: Instant): PrescribedProblem? {
        val lastByProblem = facts.attempts.groupBy { it.problemId }.mapValues { (_, list) -> list.maxBy { it.at } }
        val failed = lastByProblem.values
            .filter { !it.accepted && Duration.between(it.at, now) <= RECENT }
            .maxByOrNull { it.at } ?: return null

        val catalog = facts.catalogs[failed.problemId]
        return PrescribedProblem(
            problemId = failed.problemId,
            reason = Reason.RECENT_FAILURE,
            detail = "${daysAgo(failed.at, now)} 틀렸고 아직 맞히지 못했습니다. 원인이 손에 있을 때 다시 봅니다.",
            competency = catalog?.competencies?.firstOrNull(),
            // 지금이다. 미룰수록 무엇을 잘못 생각했는지 흐려진다.
            nextMeasurement = now,
        )
    }

    private fun transfer(facts: Facts, now: Instant): PrescribedProblem? {
        val target = facts.pendingTransfer ?: return null
        return PrescribedProblem(
            problemId = target,
            reason = Reason.TRANSFER,
            detail = "코칭에서 배운 것이 옮겨졌는지 봅니다. 힌트 없이 풀면 가장 무거운 증거가 됩니다.",
            competency = facts.catalogs[target]?.competencies?.firstOrNull(),
            nextMeasurement = now,
        )
    }

    /**
     * 맞힌 지 복습 간격이 지난 문제.
     *
     * 힌트를 보고 맞힌 것이 먼저다. 그것은 "안다"의 증거가 약했고, 힌트 없이 다시 풀어야
     * 증거가 온전해진다. 같은 조건이면 오래된 것부터 — 가장 잊었을 것부터.
     */
    private fun reviewDue(facts: Facts, now: Instant): PrescribedProblem? {
        val lastAccepted = facts.attempts.filter { it.accepted }
            .groupBy { it.problemId }
            .mapValues { (_, list) -> list.maxBy { it.at } }
        val lastAny = facts.attempts.groupBy { it.problemId }.mapValues { (_, list) -> list.maxBy { it.at } }

        val due = lastAccepted.values
            .filter { Duration.between(it.at, now) >= LearningRhythm.REVIEW_INTERVAL }
            // 맞힌 뒤에 다시 손댄 문제는 복습 중이다. 또 권하지 않는다.
            .filter { lastAny.getValue(it.problemId).at == it.at }
            .sortedWith(compareByDescending<Attempt> { facts.helpLevel(it.problemId) }.thenBy { it.at })
            .firstOrNull() ?: return null

        val help = facts.helpLevel(due.problemId)
        return PrescribedProblem(
            problemId = due.problemId,
            reason = if (help > 0) Reason.HINT_DEPENDENT else Reason.REVIEW_DUE,
            detail = if (help > 0) {
                "${daysAgo(due.at, now)} 힌트 ${help}단계를 보고 맞혔습니다. 이번엔 없이 풀어 봅니다."
            } else {
                "${daysAgo(due.at, now)} 맞혔습니다. 잊기 전에 한 번 더 봅니다."
            },
            competency = facts.catalogs[due.problemId]?.competencies?.firstOrNull(),
            nextMeasurement = now.plus(LearningRhythm.REVIEW_INTERVAL),
        )
    }

    /**
     * 약한 역량을 요구하는, 아직 안 푼, 선수를 다 푼 문제 중 가장 쉬운 것.
     *
     * DEVELOPING 만 본다. UNMEASURED 는 약한 것이 아니라 모르는 것이고, 모르는 것을
     * "약점 보완"이라 부르면 사용자는 재지도 않은 것을 못한다고 듣는다.
     */
    private fun weakCompetency(facts: Facts, now: Instant, taken: (String) -> Boolean): PrescribedProblem? {
        val weak = facts.standing.filterValues { it == Standing.DEVELOPING }.keys
        if (weak.isEmpty()) return null

        val (id, catalog) = openProblems(facts, taken)
            .filter { (_, catalog) -> catalog.competencies.any { it in weak } }
            .sortedWith(compareBy<Map.Entry<String, ProblemCatalog>> { it.value.difficulty }.thenBy { it.key })
            .firstOrNull() ?: return null
        val competency = catalog.competencies.first { it in weak }

        return PrescribedProblem(
            problemId = id,
            reason = Reason.WEAK_COMPETENCY,
            detail = "${competency.label} 역량이 아직 흔들립니다. 이 문제가 그것을 요구합니다.",
            competency = competency,
            nextMeasurement = now.plus(LearningRhythm.REVIEW_INTERVAL),
        )
    }

    /** 선수를 다 풀어 열린 문제 중 가장 쉬운 것 (§8.1 선수 관계 기반 학습 경로). */
    private fun nextOnPath(facts: Facts, now: Instant, taken: (String) -> Boolean): PrescribedProblem? {
        val (id, catalog) = openProblems(facts, taken)
            .sortedWith(compareBy<Map.Entry<String, ProblemCatalog>> { it.value.difficulty }.thenBy { it.key })
            .firstOrNull() ?: return null

        return PrescribedProblem(
            problemId = id,
            reason = Reason.NEXT_ON_PATH,
            detail = if (catalog.prerequisites.isEmpty()) {
                "선수 문제가 없는 문제 중 가장 쉬운 것입니다."
            } else {
                "선수 문제(${catalog.prerequisites.joinToString(", ")})를 다 풀어 이제 열렸습니다."
            },
            competency = catalog.competencies.firstOrNull(),
            nextMeasurement = now.plus(LearningRhythm.REVIEW_INTERVAL),
        )
    }

    /** 안 풀었고, 선수를 다 풀었고, 오늘 밀어내지 않은 문제. */
    private fun openProblems(facts: Facts, taken: (String) -> Boolean) =
        facts.catalogs.entries
            .filter { (id, _) -> id !in facts.solved && !taken(id) }
            .filter { (_, catalog) -> catalog.prerequisites.all { it in facts.solved } }

    /**
     * 스트릭. 오늘부터 거슬러 올라가며 센다.
     *
     * 하루 빈 날은 건너뛰되 세지 않고, 이틀 연속 비면 거기서 끊는다. 오늘은 아직 안 끝났으니
     * 비어 있어도 끊지 않는다.
     */
    internal fun streakOf(attempts: List<Attempt>, now: Instant, zone: ZoneId): Streak {
        val today = LocalDate.ofInstant(now, zone)
        val active = attempts.map { LocalDate.ofInstant(it.at, zone) }.toSet()

        var days = 0
        var gap = 0
        var cursor = today
        while (true) {
            when {
                cursor in active -> { days += 1; gap = 0 }
                cursor == today -> Unit // 오늘은 아직 안 끝났다
                else -> { gap += 1; if (gap >= 2) break }
            }
            cursor = cursor.minusDays(1)
            if (cursor.isBefore(today.minusDays(365))) break
        }

        return Streak(
            days = days,
            activeToday = today in active,
            atRisk = today !in active && today.minusDays(1) !in active && days > 0,
        )
    }

    private fun daysAgo(at: Instant, now: Instant): String {
        val days = Duration.between(at, now).toDays()
        return when {
            days == 0L -> "오늘"
            days == 1L -> "어제"
            else -> "${days}일 전"
        }
    }
}
