package dev.codedrill.controlplane.learning

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.Complexity
import dev.codedrill.platform.problempackage.ComplexityNote
import dev.codedrill.platform.problempackage.Difficulty
import dev.codedrill.platform.problempackage.ProblemCatalog
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 처방 규칙 (PRD FR-808, 기획서 §8.1).
 *
 * 여기서 정하는 것은 **무엇이 먼저인가**다. 이유의 순서가 곧 제품의 판단이고, 그 판단이
 * 코드 속에만 있으면 바뀌어도 아무도 모른다.
 */
class PrescriberTest {

    private val now = Instant.parse("2026-09-11T09:00:00Z")
    private val zone = ZoneOffset.UTC

    private val catalogs = mapOf(
        "a" to catalog(Difficulty.INTRO, listOf(Competency.MODELING)),
        "b" to catalog(Difficulty.EASY, listOf(Competency.EDGE_CASES), prerequisites = listOf("a")),
        "c" to catalog(Difficulty.MEDIUM, listOf(Competency.MODELING), prerequisites = listOf("a")),
        "d" to catalog(Difficulty.INTRO, listOf(Competency.COMPLEXITY)),
    )

    @Test
    fun `최근에 틀린 문제가 맨 앞이다`() {
        val facts = facts(attempts = listOf(attempt("a", accepted = false, ago = Duration.ofDays(1))))

        val items = Prescriber.prescribe(facts, now, zone).items

        assertEquals("a", items.first().problemId)
        assertEquals(Reason.RECENT_FAILURE, items.first().reason)
        assertEquals(now, items.first().nextMeasurement)
    }

    @Test
    fun `틀린 뒤에 맞혔으면 최근 오답이 아니다`() {
        val facts = facts(
            attempts = listOf(
                attempt("a", accepted = false, ago = Duration.ofDays(2)),
                attempt("a", accepted = true, ago = Duration.ofDays(1)),
            ),
            solved = setOf("a"),
        )

        assertTrue(Prescriber.prescribe(facts, now, zone).items.none { it.reason == Reason.RECENT_FAILURE })
    }

    @Test
    fun `복습은 힌트 보고 맞힌 것부터`() {
        val facts = facts(
            attempts = listOf(
                attempt("a", accepted = true, ago = Duration.ofDays(10)),
                attempt("d", accepted = true, ago = Duration.ofDays(20)),
            ),
            solved = setOf("a", "d"),
            helpLevel = { if (it == "a") 2 else 0 },
        )

        val review = Prescriber.prescribe(facts, now, zone).items
            .first { it.reason == Reason.HINT_DEPENDENT || it.reason == Reason.REVIEW_DUE }

        // d 가 더 오래됐지만 a 는 힌트를 보고 맞힌 것이라 먼저다.
        assertEquals("a", review.problemId)
        assertEquals(Reason.HINT_DEPENDENT, review.reason)
    }

    @Test
    fun `복습 간격이 안 지났으면 권하지 않는다`() {
        val facts = facts(
            attempts = listOf(attempt("a", accepted = true, ago = Duration.ofDays(3))),
            solved = setOf("a"),
        )

        assertTrue(Prescriber.prescribe(facts, now, zone).items.none { it.problemId == "a" })
    }

    @Test
    fun `약점 보완은 선수를 다 푼 문제만 권한다`() {
        // MODELING 이 약하다. 그것을 요구하는 안 푼 문제는 a 와 c 인데 c 는 a 가 선수다.
        val facts = facts(standing = mapOf(Competency.MODELING to Standing.DEVELOPING))

        val weak = Prescriber.prescribe(facts, now, zone).items.first { it.reason == Reason.WEAK_COMPETENCY }

        assertEquals("a", weak.problemId)
        assertEquals(Competency.MODELING, weak.competency)
    }

    @Test
    fun `아직 재지 않은 역량은 약점이 아니다`() {
        val facts = facts(standing = mapOf(Competency.MODELING to Standing.UNMEASURED))

        assertTrue(Prescriber.prescribe(facts, now, zone).items.none { it.reason == Reason.WEAK_COMPETENCY })
    }

    @Test
    fun `밀어낸 문제는 다시 나오지 않고 다음 후보가 올라온다`() {
        val facts = facts(skipped = setOf("a"))

        val items = Prescriber.prescribe(facts, now, zone).items

        assertFalse(items.any { it.problemId == "a" })
        // a 를 빼면 선수 없이 열린 것은 d 뿐이다.
        assertEquals("d", items.first { it.reason == Reason.NEXT_ON_PATH }.problemId)
    }

    @Test
    fun `하루에 셋을 넘지 않고 같은 문제가 두 번 나오지 않는다`() {
        val facts = facts(
            attempts = listOf(attempt("a", accepted = false, ago = Duration.ofDays(1))),
            pendingTransfer = "a",
            standing = mapOf(Competency.MODELING to Standing.DEVELOPING, Competency.COMPLEXITY to Standing.DEVELOPING),
        )

        val items = Prescriber.prescribe(facts, now, zone).items

        assertTrue(items.size <= Prescriber.DAILY_LIMIT)
        assertEquals(items.size, items.map { it.problemId }.distinct().size)
    }

    @Test
    fun `같은 상태에서는 같은 처방이 나온다`() {
        val facts = facts(standing = mapOf(Competency.MODELING to Standing.DEVELOPING))
        assertEquals(Prescriber.prescribe(facts, now, zone), Prescriber.prescribe(facts, now, zone))
    }

    @Test
    fun `스트릭은 하루 빠져도 끊기지 않고 이틀 빠지면 끊긴다`() {
        val day = Duration.ofDays(1)
        // 오늘·어제 활동, 그제 빈 날, 3·4일 전 활동, 5·6일 전 빈 날, 7일 전 활동
        val attempts = listOf(0, 1, 3, 4, 7).map { attempt("a", accepted = true, ago = day.multipliedBy(it.toLong())) }

        val streak = Prescriber.streakOf(attempts, now, zone)

        // 0,1,(2 빈),3,4 까지 넷. 5·6 이틀 비어 7 은 끊겼다.
        assertEquals(4, streak.days)
        assertTrue(streak.activeToday)
        assertFalse(streak.atRisk)
    }

    @Test
    fun `어제를 빠뜨렸으면 오늘이 위험하다고 미리 말한다`() {
        val attempts = listOf(2, 3).map { attempt("a", accepted = true, ago = Duration.ofDays(it.toLong())) }

        val streak = Prescriber.streakOf(attempts, now, zone)

        assertEquals(2, streak.days)
        assertTrue(streak.atRisk)
    }

    private fun facts(
        attempts: List<Attempt> = emptyList(),
        solved: Set<String> = emptySet(),
        helpLevel: (String) -> Int = { 0 },
        pendingTransfer: String? = null,
        standing: Map<Competency, Standing> = emptyMap(),
        skipped: Set<String> = emptySet(),
    ) = Prescriber.Facts(attempts, solved, helpLevel, pendingTransfer, standing, catalogs, skipped)

    private fun attempt(problemId: String, accepted: Boolean, ago: Duration) =
        Attempt(problemId, accepted, if (accepted) "ACCEPTED" else "WRONG_ANSWER", now.minus(ago))

    private fun catalog(
        difficulty: Difficulty,
        competencies: List<Competency>,
        prerequisites: List<String> = emptyList(),
    ) = ProblemCatalog(
        difficulty = difficulty,
        tags = listOf("array"),
        competencies = competencies,
        prerequisites = prerequisites,
        complexity = ComplexityNote(Complexity.LINEAR, Complexity.CONSTANT),
    )
}
