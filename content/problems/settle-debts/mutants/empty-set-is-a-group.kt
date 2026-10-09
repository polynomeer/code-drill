// kind: OFF_BY_ONE
// 빈 부분집합의 합도 0 이라 무리 하나로 센다. 빈 무리는 송금을 아끼지 않는다 — 답이 하나 적게 나온다.
fun settleDebts(transactions: IntArray): Int {
    val balance = IntArray(20)
    for (i in transactions.indices step 3) { balance[transactions[i]] -= transactions[i + 2]; balance[transactions[i + 1]] += transactions[i + 2] }
    val values = balance.filter { it != 0 }
    val k = values.size
    val full = 1 shl k
    val sums = IntArray(full)
    val groups = IntArray(full)
    groups[0] = 1
    for (mask in 1 until full) {
        sums[mask] = sums[mask and (mask - 1)] + values[Integer.numberOfTrailingZeros(mask)]
        var best = 0
        for (i in 0 until k) if (mask shr i and 1 == 1) best = maxOf(best, groups[mask xor (1 shl i)])
        groups[mask] = best + if (sums[mask] == 0) 1 else 0
    }
    return maxOf(0, k - groups[full - 1])
}
