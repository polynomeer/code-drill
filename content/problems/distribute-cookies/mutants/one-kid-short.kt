// kind: OFF_BY_ONE
// 아이를 k - 1 명으로 나눈다. 반복을 한 번 덜 돈다.
fun distributeCookies(bags: IntArray, k: Int): Int {
    val n = bags.size; val full = 1 shl n
    val sums = IntArray(full)
    for (mask in 1 until full) sums[mask] = sums[mask and (mask - 1)] + bags[Integer.numberOfTrailingZeros(mask)]
    var best = sums.copyOf()
    repeat(k - 2) {
        val next = IntArray(full)
        for (mask in 0 until full) {
            var value = best[mask]; var sub = mask
            while (sub != 0) { val c = maxOf(best[mask xor sub], sums[sub]); if (c < value) value = c; sub = (sub - 1) and mask }
            next[mask] = value
        }
        best = next
    }
    return best[full - 1]
}
