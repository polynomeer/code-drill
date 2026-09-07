// 검증용 정답 (§6.1 solutions/). 금액을 1 씩 올려 가는 DP.
//
// 욕심껏 큰 동전을 집으면 안 된다. 어떤 금액의 최소 개수는 "그보다 동전 하나만큼
// 작은 금액들" 중 가장 좋은 것에 1 을 더한 값이며, 그 후보를 전부 봐야 한다.
fun coinChange(coins: IntArray, amount: Int): Int {
    val unreachable = amount + 1
    val best = IntArray(amount + 1) { unreachable }
    best[0] = 0

    for (value in 1..amount) {
        for (coin in coins) {
            if (coin > value) continue
            Drill.compare(value, coin)
            val candidate = best[value - coin] + 1
            if (candidate < best[value]) {
                best[value] = candidate
                Drill.write(value, candidate)
            }
        }
    }
    return if (best[amount] > amount) -1 else best[amount]
}
