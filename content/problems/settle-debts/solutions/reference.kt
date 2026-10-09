// 검증용 정답 (§6.1 solutions/). 잔액만 남기고, 부분집합마다 합이 0 인 무리로 가장 많이 쪼갠 수를 센다.
fun settleDebts(transactions: IntArray): Int {
    val balance = IntArray(20)
    for (i in transactions.indices step 3) {
        balance[transactions[i]] -= transactions[i + 2]
        balance[transactions[i + 1]] += transactions[i + 2]
    }
    val values = balance.filter { it != 0 }
    val k = values.size
    val full = 1 shl k
    val sums = IntArray(full)
    val groups = IntArray(full)
    for (mask in 1 until full) {
        val low = Integer.numberOfTrailingZeros(mask)
        sums[mask] = sums[mask and (mask - 1)] + values[low]
        var best = 0
        var rest = mask
        while (rest != 0) {
            val bit = rest and -rest
            if (groups[mask xor bit] > best) best = groups[mask xor bit]
            rest = rest xor bit
        }
        groups[mask] = best + if (sums[mask] == 0) 1 else 0
        if (sums[mask] == 0) Drill.write(mask, groups[mask])
    }
    return k - groups[full - 1]
}
