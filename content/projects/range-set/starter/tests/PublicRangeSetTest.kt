package tests

import codedrill.*
import ranges.RangeSet
import ranges.Span

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicRangeSetTest {

    fun testOverlappingAddsMerge() {
        val set = RangeSet()
        set.add(1, 5)
        set.add(3, 8)
        set.add(10, 12)
        assertEquals(listOf(Span(1, 8), Span(10, 12)), set.spans())
    }

    fun testContainsIsHalfOpen() {
        val set = RangeSet()
        set.add(1, 5)
        assertTrue(set.contains(1))
        assertTrue(set.contains(4))
        assertFalse(set.contains(5))
    }

    fun testRemoveTrimsAnEnd() {
        val set = RangeSet()
        set.add(1, 10)
        set.remove(7, 20)
        assertEquals(listOf(Span(1, 7)), set.spans())
        assertEquals(6L, set.size())
    }
}
