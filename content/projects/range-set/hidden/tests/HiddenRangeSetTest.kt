package tests

import codedrill.*
import ranges.RangeSet
import ranges.Span

/** 숨은 테스트. 사용자에게 나가지 않는다. */
class HiddenRangeSetTest {

    private fun of(vararg pairs: Long): RangeSet {
        val set = RangeSet()
        for (i in pairs.indices step 2) set.add(pairs[i], pairs[i + 1])
        return set
    }

    fun testTouchingSpansMerge() {
        assertEquals(listOf(Span(1, 5)), of(1, 3, 3, 5).spans())
        assertEquals(listOf(Span(1, 5)), of(3, 5, 1, 3).spans())
    }

    fun testOneAddBridgesMany() {
        val set = of(1, 2, 4, 5, 7, 8, 10, 11)
        set.add(2, 7)
        assertEquals(listOf(Span(1, 8), Span(10, 11)), set.spans())
    }

    fun testAddInsideAndAround() {
        val set = of(0, 10)
        set.add(3, 4)
        assertEquals(listOf(Span(0, 10)), set.spans())
        set.add(-5, 20)
        assertEquals(listOf(Span(-5, 20)), set.spans())
    }

    fun testEmptyIsANoOp() {
        val set = of(1, 3)
        set.add(5, 5)
        set.remove(2, 2)
        assertEquals(listOf(Span(1, 3)), set.spans())
        assertTrue(set.covers(9, 9))
    }

    fun testReversedIsRejected() {
        val set = RangeSet()
        assertThrows<IllegalArgumentException> { set.add(5, 4) }
        assertThrows<IllegalArgumentException> { set.remove(5, 4) }
        assertThrows<IllegalArgumentException> { set.covers(5, 4) }
    }

    fun testRemoveSplits() {
        val set = of(0, 10)
        set.remove(3, 6)
        assertEquals(listOf(Span(0, 3), Span(6, 10)), set.spans())
        assertFalse(set.contains(3))
        assertTrue(set.contains(6))
    }

    fun testRemoveAcrossSeveral() {
        val set = of(0, 2, 4, 6, 8, 10)
        set.remove(1, 9)
        assertEquals(listOf(Span(0, 1), Span(9, 10)), set.spans())
    }

    fun testRemoveStartingAtASpanStart() {
        val set = of(0, 10)
        set.remove(0, 4)
        assertEquals(listOf(Span(4, 10)), set.spans())
    }

    fun testRemoveTouchingDoesNothing() {
        val set = of(1, 5)
        set.remove(5, 7)
        set.remove(-3, 1)
        assertEquals(listOf(Span(1, 5)), set.spans())
    }

    fun testRemoveEverything() {
        val set = of(1, 5, 7, 9)
        set.remove(0, 100)
        assertEquals(emptyList<Span>(), set.spans())
        assertEquals(0L, set.size())
    }

    fun testCoversWithinOneSpan() {
        val set = of(0, 10, 20, 30)
        assertTrue(set.covers(0, 10))
        assertTrue(set.covers(22, 25))
        assertFalse(set.covers(5, 25))
        assertFalse(set.covers(9, 11))
    }

    fun testCoversAfterMerge() {
        val set = of(0, 5, 5, 10)
        assertTrue(set.covers(2, 8))
    }

    fun testHugeCoordinates() {
        val set = of(-1_000_000_000_000_000_000, 1_000_000_000_000_000_000)
        assertEquals(2_000_000_000_000_000_000, set.size())
        set.remove(0, 1)
        assertEquals(1_999_999_999_999_999_999, set.size())
        assertTrue(set.contains(-1_000_000_000_000_000_000))
        assertFalse(set.contains(1_000_000_000_000_000_000))
    }

    fun testRandomAgainstAModel() {
        val random = kotlin.random.Random(20261010)
        val set = RangeSet()
        val model = BooleanArray(200)
        repeat(3000) {
            val a = random.nextInt(0, 200).toLong()
            val b = random.nextInt(0, 200).toLong()
            val from = minOf(a, b)
            val to = maxOf(a, b)
            when (random.nextInt(4)) {
                0, 1 -> { set.add(from, to); for (x in from until to) model[x.toInt()] = true }
                2 -> { set.remove(from, to); for (x in from until to) model[x.toInt()] = false }
                else -> {
                    val expected = (from until to).all { model[it.toInt()] }
                    assertEquals(expected, set.covers(from, to))
                }
            }
            val x = random.nextInt(0, 200)
            assertEquals(model[x], set.contains(x.toLong()))
        }
        val expectedSpans = ArrayList<Span>()
        var i = 0
        while (i < 200) {
            if (!model[i]) { i += 1; continue }
            var j = i
            while (j < 200 && model[j]) j += 1
            expectedSpans.add(Span(i.toLong(), j.toLong()))
            i = j
        }
        assertEquals(expectedSpans, set.spans())
        assertEquals(model.count { it }.toLong(), set.size())
    }
}
