// kind: MISSING_EDGE_CASE
// 금액이 0 일 때 0 이 아닌 값을 돌려준다.
fun coinChange(coins: IntArray, amount: Int): Int {
    val unreachable = amount + 1
    val best = IntArray(amount + 1) { unreachable }
    for (value in 1..amount) {
        for (coin in coins) {
            if (coin > value) continue
            if (coin == value) { best[value] = 1; continue }
            val candidate = best[value - coin] + 1
            if (candidate < best[value]) best[value] = candidate
        }
    }
    return if (amount == 0) 1 else if (best[amount] > amount) -1 else best[amount]
}
