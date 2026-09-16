package dev.codedrill.controlplane.contest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EloTest {

    @Test
    fun `같은 레이팅의 둘이 겨루면 이긴 쪽이 32 를 얻고 진 쪽이 32 를 잃는다`() {
        val changes = Elo.changes(listOf(Elo.Player("a", 1500, 100, 10), Elo.Player("b", 1500, 0, null)))
        assertEquals(32, changes["a"])
        assertEquals(-32, changes["b"])
    }

    @Test
    fun `동점이면 아무도 움직이지 않는다`() {
        val changes = Elo.changes(listOf(Elo.Player("a", 1500, 100, 10), Elo.Player("b", 1500, 100, 10)))
        assertEquals(0, changes["a"])
        assertEquals(0, changes["b"])
    }

    @Test
    fun `높은 쪽이 이기면 조금 얻고, 지면 많이 잃는다`() {
        val expectedWin = Elo.changes(listOf(Elo.Player("a", 1800, 100, 10), Elo.Player("b", 1500, 0, null)))
        val upset = Elo.changes(listOf(Elo.Player("a", 1800, 0, null), Elo.Player("b", 1500, 100, 10)))
        assertTrue(expectedWin["a"]!! in 1..20, "예상된 승리: ${expectedWin["a"]}")
        assertTrue(upset["a"]!! <= -40, "이변: ${upset["a"]}")
    }

    @Test
    fun `참가자가 많아도 변화는 K 를 넘지 않고, 합은 0 근처다`() {
        val players = (0 until 50).map { Elo.Player("u$it", 1500, 100 - it, it.toLong()) }
        val changes = Elo.changes(players)
        assertTrue(changes.values.all { it in -64..64 })
        assertTrue(kotlin.math.abs(changes.values.sum()) <= players.size, "합: ${changes.values.sum()}")
    }

    @Test
    fun `혼자면 아무것도 없다`() {
        assertEquals(emptyMap(), Elo.changes(listOf(Elo.Player("a", 1500, 100, 1))))
    }
}
