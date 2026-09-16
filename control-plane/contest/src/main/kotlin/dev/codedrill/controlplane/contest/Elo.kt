package dev.codedrill.controlplane.contest

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 여러 명의 Elo (§8.4 정기 레이팅).
 *
 * 참가자마다 나머지 전원과의 "기대 승률"을 평균하고, 실제 순위에서 온 "실제 승률"과의 차에
 * K 를 곱한다. 둘이면 보통의 Elo 이고, 많으면 변화가 참가자 수에 따라 부풀지 않는다 —
 * 평균이기 때문이다. 동점은 반 승이다.
 *
 * 첫 버전이다. 원본(§10.2)은 표본이 쌓인 뒤 보정하라고 했고, 변화는 전부 남으므로 식을
 * 바꾸면 다시 셀 수 있다.
 */
object Elo {

    const val INITIAL = 1500
    const val K = 64.0

    /** 참가자 하나. [key] 는 순위의 비교 값 — 같으면 동점이다. */
    data class Player(val userId: String, val rating: Int, val total: Int, val elapsedSeconds: Long?)

    /** 변화. 참가자가 하나뿐이면 아무것도 바뀌지 않는다 — 견줄 상대가 없다. */
    fun changes(players: List<Player>): Map<String, Int> {
        if (players.size < 2) return emptyMap()
        return players.associate { me ->
            var expected = 0.0
            var actual = 0.0
            for (other in players) {
                if (other === me) continue
                expected += 1.0 / (1.0 + 10.0.pow((other.rating - me.rating) / 400.0))
                actual += when (compare(me, other)) { 1 -> 1.0; 0 -> 0.5; else -> 0.0 }
            }
            val n = players.size - 1
            me.userId to (K * (actual / n - expected / n)).roundToInt()
        }
    }

    /** 순위의 비교: 총점 높은 쪽, 같으면 걸린 시간 짧은 쪽. 둘 다 같으면 동점. */
    private fun compare(a: Player, b: Player): Int {
        if (a.total != b.total) return if (a.total > b.total) 1 else -1
        val ea = a.elapsedSeconds ?: Long.MAX_VALUE
        val eb = b.elapsedSeconds ?: Long.MAX_VALUE
        return when { ea < eb -> 1; ea > eb -> -1; else -> 0 }
    }
}
