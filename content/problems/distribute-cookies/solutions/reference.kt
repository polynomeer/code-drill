// 검증용 정답 (§6.1 solutions/). 봉지 집합 비트마스크 DP — 아이를 한 명씩 더하며 그 아이가 받을 부분집합을 모두 본다.
fun distributeCookies(bags: IntArray, k: Int): Int {
    val n = bags.size
    val full = 1 shl n
    val sums = IntArray(full)
    for (mask in 1 until full) sums[mask] = sums[mask and (mask - 1)] + bags[Integer.numberOfTrailingZeros(mask)]
    var best = sums.copyOf()
    repeat(k - 1) {
        val next = IntArray(full)
        for (mask in 0 until full) {
            var value = best[mask]
            var sub = mask
            while (sub != 0) {
                val candidate = maxOf(best[mask xor sub], sums[sub])
                if (candidate < value) value = candidate
                sub = (sub - 1) and mask
            }
            next[mask] = value
        }
        best = next
    }
    Drill.write(full - 1, best[full - 1])
    return best[full - 1]
}
