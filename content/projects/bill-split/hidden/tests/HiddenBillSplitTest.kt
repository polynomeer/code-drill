package tests

import codedrill.*
import split.BillSplitter
import split.Share

/** 숨은 테스트. 사용자에게 나가지 않는다 (§8.3). */
class HiddenBillSplitTest {

    private val splitter = BillSplitter()

    fun testLeftoverFollowsGivenOrderNotNames() {
        val shares = splitter.splitEvenly(5, listOf("z", "a", "m", "b"))
        assertEquals(listOf(Share("z", 2), Share("a", 1), Share("m", 1), Share("b", 1)), shares)
    }

    fun testEvenSplitAlwaysAddsUp() {
        for (total in 0L..60L) for (n in 1..7) {
            val shares = splitter.splitEvenly(total, (1..n).map { "p$it" })
            assertEquals(total, shares.sumOf { it.amount })
            assertTrue(shares.maxOf { it.amount } - shares.minOf { it.amount } <= 1)
        }
    }

    fun testZeroTotal() {
        assertEquals(listOf(Share("a", 0), Share("b", 0)), splitter.splitEvenly(0, listOf("a", "b")))
        assertEquals(listOf(Share("a", 0), Share("b", 0)), splitter.splitByWeight(0, listOf("a" to 1, "b" to 2)))
    }

    fun testWeightedTieGoesToFrontPerson() {
        val shares = splitter.splitByWeight(100, listOf("a" to 1, "b" to 1, "c" to 1))
        assertEquals(listOf(Share("a", 34), Share("b", 33), Share("c", 33)), shares)
    }

    fun testWeightedLargestRemainderWins() {
        // 정확한 몫 1.4, 2.1, 3.5 — 내림 합이 6 이라 남은 1원은 소수 부분이 가장 큰 c 에게
        val shares = splitter.splitByWeight(7, listOf("a" to 2, "b" to 3, "c" to 5))
        assertEquals(listOf(Share("a", 1), Share("b", 2), Share("c", 4)), shares)
    }

    fun testWeightedAlwaysAddsUp() {
        val weights = listOf("a" to 3, "b" to 3, "c" to 3, "d" to 7, "e" to 11)
        for (total in 0L..200L) {
            assertEquals(total, splitter.splitByWeight(total, weights).sumOf { it.amount })
        }
    }

    fun testZeroWeightPaysNothing() {
        assertEquals(listOf(Share("a", 0), Share("b", 10)), splitter.splitByWeight(10, listOf("a" to 0, "b" to 1)))
    }

    fun testLargeTotalsStayExact() {
        // 곱이 10^17 을 넘으면 Double 은 1원을 잃는다
        assertEquals(
            listOf(Share("a", 324_078_281_804), Share("b", 200_762_385_444), Share("c", 125_686_060_150)),
            splitter.splitByWeight(650_526_727_398, listOf("a" to 981_192, "b" to 607_836, "c" to 380_532)),
        )
        assertEquals(
            listOf(Share("a", 203_227_481_407), Share("b", 273_393_378_845)),
            splitter.splitByWeight(476_620_860_252, listOf("a" to 516_440, "b" to 694_745)),
        )
        assertEquals(
            listOf(Share("a", 105_424_417_879), Share("b", 89_033_813_672), Share("c", 64_078_343_620)),
            splitter.splitByWeight(258_536_575_171, listOf("a" to 996_581, "b" to 841_640, "c" to 605_735)),
        )
        val even = splitter.splitEvenly(1_000_000_000_000, listOf("a", "b", "c"))
        assertEquals(1_000_000_000_000L, even.sumOf { it.amount })
    }

    fun testTipRoundsHalfUp() {
        assertEquals(1106L, splitter.addTip(1005, 10))
        assertEquals(999L, splitter.addTip(999, 0))
        assertEquals(0L, splitter.addTip(0, 50))
        assertEquals(2L, splitter.addTip(1, 50))
        assertEquals(2_000_000_000_000L, splitter.addTip(1_000_000_000_000, 100))
    }

    fun testRejectsBadInput() {
        assertThrows<IllegalArgumentException> { splitter.splitEvenly(10, listOf("a", "a")) }
        assertThrows<IllegalArgumentException> { splitter.splitEvenly(10, listOf("a", " ")) }
        assertThrows<IllegalArgumentException> { splitter.splitEvenly(1_000_000_000_001, listOf("a")) }
        assertThrows<IllegalArgumentException> { splitter.splitByWeight(10, listOf("a" to 1, "a" to 2)) }
        assertThrows<IllegalArgumentException> { splitter.splitByWeight(10, listOf("a" to 0, "b" to 0)) }
        assertThrows<IllegalArgumentException> { splitter.splitByWeight(10, listOf("a" to -1, "b" to 2)) }
        assertThrows<IllegalArgumentException> { splitter.splitByWeight(10, listOf("a" to 1_000_001)) }
        assertThrows<IllegalArgumentException> { splitter.splitByWeight(10, emptyList()) }
        assertThrows<IllegalArgumentException> { splitter.addTip(10, 101) }
        assertThrows<IllegalArgumentException> { splitter.addTip(10, -1) }
        assertThrows<IllegalArgumentException> { splitter.addTip(-1, 10) }
    }
}
