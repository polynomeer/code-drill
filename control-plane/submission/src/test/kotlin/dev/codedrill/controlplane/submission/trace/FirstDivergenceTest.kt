package dev.codedrill.controlplane.submission.trace

import dev.codedrill.judge.protocol.TraceEvent
import dev.codedrill.judge.protocol.TraceEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 상태 계약 비교 (PRD FR-805).
 *
 * 여기서 정하는 것은 **무엇을 같다고 볼 것인가**다. 너무 좁게 잡으면 모든 제출이 1번에서
 * 갈리고, 너무 넓게 잡으면 진짜 버그가 같은 것으로 보인다. 그 경계가 이 스위트의 전부다.
 */
class FirstDivergenceTest {

    @Test
    fun `같은 열은 갈라지지 않는다`() {
        val events = listOf(visit(1, "0", "2"), visit(2, "1", "7"))
        assertNull(FirstDivergence.of(events, events.map { it.contractKey() }))
    }

    @Test
    fun `seq 와 코드 줄은 비교에 들어가지 않는다`() {
        // 계측 호출을 한 줄 아래로 옮겼다고 갈라진 것이 아니다. 그것으로 비교하면
        // 코드를 정리한 사람이 벌을 받는다.
        val mine = listOf(
            visit(10, "0", "2", line = 7),
            visit(11, "1", "7", line = 9),
        )
        val reference = listOf(
            visit(1, "0", "2", line = 3),
            visit(2, "1", "7", line = 4),
        ).map { it.contractKey() }

        assertNull(FirstDivergence.of(mine, reference))
    }

    @Test
    fun `값이 다르면 그 자리가 분기다`() {
        val mine = listOf(visit(1, "0", "2"), visit(2, "1", "9"))
        val reference = listOf(visit(1, "0", "2"), visit(2, "1", "7")).map { it.contractKey() }

        val point = FirstDivergence.of(mine, reference)!!

        assertEquals(1, point.index)
        assertEquals(2L, point.userEvent?.seq)
        assertTrue(point.referenceKey!!.endsWith("|7"))
    }

    @Test
    fun `대상이 다르면 값이 같아도 분기다`() {
        // 3번을 봐야 할 자리에서 4번을 봤다. 값이 우연히 같아도 다른 일을 한 것이다.
        val mine = listOf(visit(1, "4", "5"))
        val reference = listOf(visit(1, "3", "5")).map { it.contractKey() }

        assertEquals(0, FirstDivergence.of(mine, reference)?.index)
    }

    @Test
    fun `내 쪽이 먼저 끝나도 분기다`() {
        val mine = listOf(visit(1, "0", "2"))
        val reference = listOf(visit(1, "0", "2"), visit(2, "1", "7")).map { it.contractKey() }

        val point = FirstDivergence.of(mine, reference)!!

        assertEquals(1, point.index)
        // 내 이벤트가 없다는 것 자체가 사실이다. 화면이 "여기서 끝났다"를 말할 수 있어야 한다.
        assertNull(point.userEvent)
        assertTrue(point.referenceKey != null)
    }

    @Test
    fun `참조가 먼저 끝나도 분기다`() {
        val mine = listOf(visit(1, "0", "2"), visit(2, "1", "7"))
        val reference = listOf(visit(1, "0", "2")).map { it.contractKey() }

        val point = FirstDivergence.of(mine, reference)!!

        assertEquals(1, point.index)
        assertNull(point.referenceKey)
        assertEquals(2L, point.userEvent?.seq)
    }

    @Test
    fun `참조가 비어 있으면 첫 이벤트에서 갈린다`() {
        // 이 경우를 "다른 접근"으로 부른다. 참조가 계측을 부르지 않은 것이라 짚을 자리가 없다.
        val point = FirstDivergence.of(listOf(visit(1, "0", "2")), emptyList())!!
        assertEquals(0, point.index)
    }

    @Test
    fun `사람이 읽을 한 줄에 대상과 결과가 들어간다`() {
        assertEquals("VISIT array[3] → 9", visit(1, "3", "9").describe())
    }

    private fun visit(seq: Long, target: String, after: String, line: Int? = null) = TraceEvent(
        seq = seq,
        logicalTime = seq,
        eventType = TraceEventType.VISIT,
        targetKind = TraceEventType.VISIT.kind,
        targetRef = target,
        after = after,
        sourceLine = line,
    )
}
