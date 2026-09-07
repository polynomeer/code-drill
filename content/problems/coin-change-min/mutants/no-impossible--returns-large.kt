// kind: MISSING_EDGE_CASE
// 만들 수 없을 때 -1 대신 큰 수를 돌려준다.
fun coinChange(coins: IntArray, amount: Int): Int {
    val unreachable = amount + 1
    val best = IntArray(amount + 1) { unreachable }
    best[0] = 0
    for (value in 1..amount) {
        for (coin in coins) {
            if (coin > value) continue
            val candidate = best[value - coin] + 1
            if (candidate < best[value]) best[value] = candidate
        }
    }
    return best[amount]
}
