package dev.codedrill.controlplane.coaching

import dev.codedrill.platform.problempackage.Competency
import dev.codedrill.platform.problempackage.ProblemPackageLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 도움 사다리 (FR-802).
 *
 * 저장소의 실제 콘텐츠로 돈다. 가짜 문제로 시험하면 "코드를 주지 않는다"를 확인할 수
 * 없다 — 그 규칙이 깨지는 자리는 **실제 오답 설명이 얼마나 자세한가**이고, 그것은 지어낸
 * 문제에는 없다.
 */
class HintLadderTest {

    private val ladder = HintLadder(ProblemPackageLoader(Path.of("../../content/problems")))

    @Test
    fun `복잡도 사다리는 목표를 말하되 방법을 말하지 않는다`() {
        val steps = ladder.of("two-sum", Competency.COMPLEXITY)

        // 단계 번호는 1부터 빈틈없이 이어져야 한다. two-sum 처럼 등급과 식이 같은 문제는
        // 가운데 단계가 빠지는데, 그때 번호가 1·3 이 되면 화면이 없는 2단계를 기다린다.
        assertEquals(steps.indices.map { it + 1 }, steps.map { it.level })
        assertTrue(steps.first().text.contains("O(n)"))
        // 코드를 주면 그것은 도움이 아니라 정답이다.
        assertTrue(steps.none { it.text.contains("fun ") || it.text.contains("HashMap") })
    }

    @Test
    fun `등급과 실제 식이 다르면 그 식을 한 단계로 넣는다`() {
        // 편집 거리는 등급으로 제곱이지만 실제로는 O(n·m) 이다. 등급만 말하면 화면이
        // 거짓을 말한다.
        val steps = ladder.of("edit-distance", Competency.COMPLEXITY)

        assertTrue(steps.any { it.text.contains("O(n·m)") })
    }

    @Test
    fun `엣지케이스 사다리는 저작자가 오답에 적어 둔 문장에서 나온다`() {
        val steps = ladder.of("two-sum", Competency.EDGE_CASES)

        assertTrue(steps.any { it.text.contains("마지막 원소") })
        assertTrue(steps.any { it.text.contains("같은 원소를 두 번") })
        // 결함군 이름을 붙여 "무엇을 더 시험해야 하나"로 읽히게 한다.
        assertTrue(steps.all { it.text.contains("(") })
    }

    @Test
    fun `성능 오답은 정확성 사다리에 오르지 않는다`() {
        // max-subarray 에는 PERFORMANCE 오답이 하나 있다. 큰 입력에서 느리다는 사실은
        // "답이 맞는가"에 아무 단서도 주지 않는다.
        val steps = ladder.of("max-subarray", Competency.EDGE_CASES)

        assertTrue(steps.isNotEmpty())
        assertFalse(steps.any { it.text.contains("시간 초과") })
    }

    @Test
    fun `오답 설명의 뒷문장은 힌트가 되지 않는다`() {
        // row-wrap 설명은 뒤 두 문장이 그 오답의 구현을 설명한다. 힌트로는 혼란스럽다.
        val steps = ladder.of("island-perimeter", Competency.EDGE_CASES) +
            ladder.of("island-count", Competency.EDGE_CASES)

        assertTrue(steps.none { it.text.contains("평탄화") })
    }

    @Test
    fun `다룰 것이 없는 역량은 빈 사다리다`() {
        // 사다리가 없다고 지어내지 않는다. 지어낸 힌트는 문제와 어긋나고, 어긋난 힌트는
        // 없느니만 못하다.
        assertEquals(emptyList(), ladder.of("two-sum", Competency.AI_COLLABORATION))
    }

    @Test
    fun `없는 문제는 터지지 않고 빈 사다리다`() {
        assertEquals(emptyList(), ladder.of("no-such-problem", Competency.COMPLEXITY))
    }
}
