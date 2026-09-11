package dev.codedrill.controlplane.competency

import dev.codedrill.platform.problempackage.Competency
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 숙련도 투영 (PRD §3.4, FR-801·FR-806).
 *
 * 이 계산이 사용자에게 "너는 이걸 잘한다/못한다"를 말한다. 틀리면 코칭이 엉뚱한 곳을
 * 겨누므로, 경계와 표본 규칙을 고정해 둔다.
 */
class MasteryProjectionTest {

    private fun evidence(
        competency: Competency,
        success: Boolean,
        weight: Double = 1.0,
        at: Instant = Instant.now(),
    ) = Evidence(
        id = UUID.randomUUID(),
        userId = "u",
        competency = competency,
        source = EvidenceSource.SUBMISSION,
        success = success,
        weight = weight,
        problemId = "two-sum",
        reference = UUID.randomUUID().toString(),
        detail = null,
        occurredAt = at,
    )

    private fun of(vararg rows: Evidence) = MasteryProjection.of(rows.toList())
        .associateBy { it.competency }

    @Test
    fun `증거가 없으면 미측정이다`() {
        // 모르는 것을 낮은 점수로 표시하지 않는다. 0% 로 그리면 사용자는 자기가 못한다고
        // 읽는데, 사실은 아직 보인 적이 없는 것이다.
        val map = of()

        assertEquals(MasteryLevel.UNMEASURED, map.getValue(Competency.IMPLEMENTATION).level)
        assertEquals(Confidence.NONE, map.getValue(Competency.IMPLEMENTATION).confidence)
    }

    @Test
    fun `모든 역량을 돌려준다`() {
        // 목록에서 빼면 화면이 "아직 측정하지 않았다"를 말할 수 없다.
        assertEquals(Competency.entries.size, MasteryProjection.of(emptyList()).size)
    }

    @Test
    fun `절반을 넘기지 못하면 발전 중이다`() {
        val map = of(
            evidence(Competency.MODELING, success = true),
            evidence(Competency.MODELING, success = false),
            evidence(Competency.MODELING, success = false),
        )

        assertEquals(MasteryLevel.DEVELOPING, map.getValue(Competency.MODELING).level)
    }

    @Test
    fun `대체로 해내면 숙련이다`() {
        val rows = List(7) { evidence(Competency.MODELING, success = true) } +
            List(3) { evidence(Competency.MODELING, success = false) }

        assertEquals(MasteryLevel.PROFICIENT, MasteryProjection.of(rows).first { it.competency == Competency.MODELING }.level)
    }

    @Test
    fun `거의 놓치지 않으면 강함이다`() {
        val rows = List(9) { evidence(Competency.MODELING, success = true) }

        assertEquals(MasteryLevel.STRONG, MasteryProjection.of(rows).first { it.competency == Competency.MODELING }.level)
    }

    @Test
    fun `무게가 낮은 증거는 덜 센다`() {
        // 도움을 받아 맞힌 것과 혼자 맞힌 것은 같은 증거가 아니다 (§3.4).
        // 성공 하나(0.2)와 실패 하나(1.0) → 0.17 로 발전 중이다.
        val map = of(
            evidence(Competency.MODELING, success = true, weight = 0.2),
            evidence(Competency.MODELING, success = false, weight = 1.0),
        )

        assertEquals(MasteryLevel.DEVELOPING, map.getValue(Competency.MODELING).level)
    }

    @Test
    fun `표본이 적으면 신뢰도가 낮다`() {
        // 증거 둘로 얻은 STRONG 과 서른으로 얻은 STRONG 은 같은 등급이지만 같은 사실이
        // 아니다. 등급과 신뢰도를 따로 내는 이유가 이것이다.
        val two = of(
            evidence(Competency.MODELING, success = true),
            evidence(Competency.MODELING, success = true),
        ).getValue(Competency.MODELING)

        assertEquals(MasteryLevel.STRONG, two.level)
        assertEquals(Confidence.LOW, two.confidence)
    }

    @Test
    fun `표본이 쌓이면 신뢰도가 오른다`() {
        val levels = listOf(3, 9, 10).map { count ->
            MasteryProjection.of(List(count) { evidence(Competency.MODELING, success = true) })
                .first { it.competency == Competency.MODELING }
                .confidence
        }

        assertEquals(listOf(Confidence.MEDIUM, Confidence.MEDIUM, Confidence.HIGH), levels)
    }

    @Test
    fun `증거 수와 성공 수를 함께 낸다`() {
        // 화면이 "무엇에 근거했나"를 말할 수 있어야 한다 (FR-806).
        val one = of(
            evidence(Competency.MODELING, success = true),
            evidence(Competency.MODELING, success = false),
        ).getValue(Competency.MODELING)

        assertEquals(2, one.evidenceCount)
        assertEquals(1, one.successCount)
    }

    @Test
    fun `오래된 증거는 등급에는 남되 신뢰도에는 세지 않는다`() {
        // 40일 전 열 번 맞혔다. 한 달 넘게 아무것도 안 했다.
        val old = Instant.now().minus(java.time.Duration.ofDays(40))
        val rows = (1..10).map { evidence(Competency.MODELING, success = true, at = old) }

        val mastery = MasteryProjection.of(rows).first { it.competency == Competency.MODELING }

        // 한 번은 해냈다는 사실은 남는다.
        assertEquals(MasteryLevel.STRONG, mastery.level)
        // 그러나 그것을 오늘 믿을 근거는 낡았다. 재지 않은 것(NONE)과는 다르다.
        assertEquals(Confidence.LOW, mastery.confidence)
    }

    @Test
    fun `과거 시점으로 그리면 그 뒤의 증거는 없는 것이다`() {
        val now = Instant.now()
        val rows = listOf(
            evidence(Competency.MODELING, success = false, at = now.minus(java.time.Duration.ofDays(10))),
            evidence(Competency.MODELING, success = true, at = now.minus(java.time.Duration.ofDays(1))),
            evidence(Competency.MODELING, success = true, at = now.minus(java.time.Duration.ofDays(1))),
        )

        val then = MasteryProjection.of(rows, now.minus(java.time.Duration.ofDays(7)))
            .first { it.competency == Competency.MODELING }
        val today = MasteryProjection.of(rows, now).first { it.competency == Competency.MODELING }

        // 일주일 전에는 실패 하나뿐이었고, 오늘은 셋 중 둘을 해냈다. 주간 리포트의 "성장"이다.
        assertEquals(MasteryLevel.DEVELOPING, then.level)
        assertEquals(MasteryLevel.PROFICIENT, today.level)
    }
}
