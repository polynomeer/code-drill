package tests

import codedrill.*
import split.BillSplitter
import split.Share

/** 공개 테스트. 채점은 이것과 숨은 테스트를 함께 돌린다. 하네스의 단언을 쓴다. */
class PublicBillSplitTest {

    fun testEvenSplitGivesLeftoverToFrontPeople() {
        val shares = BillSplitter().splitEvenly(100, listOf("민지", "서준", "하은"))
        assertEquals(listOf(Share("민지", 34), Share("서준", 33), Share("하은", 33)), shares)
    }

    fun testWeightedSplitWhenItDividesEvenly() {
        val shares = BillSplitter().splitByWeight(1000, listOf("a" to 1, "b" to 3))
        assertEquals(listOf(Share("a", 250), Share("b", 750)), shares)
    }

    fun testTipWithoutRounding() {
        assertEquals(11_000L, BillSplitter().addTip(10_000, 10))
    }

    fun testRejectsBadInput() {
        assertThrows<IllegalArgumentException> { BillSplitter().splitEvenly(-1, listOf("a")) }
        assertThrows<IllegalArgumentException> { BillSplitter().splitEvenly(10, emptyList()) }
    }
}
