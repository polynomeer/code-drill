// 검증용 정답 (§6.1 solutions/). 걷어낸 뒤 (쓴 낱말 집합, 마지막 낱말) 표로 아낀 글자 수를 키운다.
fun shortestSuperstring(words: Array<String>): Int {
    val unique = words.distinct()
    val kept = unique.filter { w -> unique.none { it != w && it.contains(w) } }
    val n = kept.size
    val overlap = Array(n) { IntArray(n) }
    for (i in 0 until n) for (j in 0 until n) {
        if (i == j) continue
        val a = kept[i]
        val b = kept[j]
        var k = minOf(a.length, b.length) - 1
        while (k > 0 && !a.endsWith(b.substring(0, k))) k -= 1
        overlap[i][j] = k
    }
    val full = (1 shl n) - 1
    val saved = Array(1 shl n) { IntArray(n) { -1 } }
    for (i in 0 until n) saved[1 shl i][i] = 0
    for (mask in 1..full) {
        for (last in 0 until n) {
            val here = saved[mask][last]
            if (here < 0) continue
            for (next in 0 until n) {
                if (mask and (1 shl next) != 0) continue
                val to = mask or (1 shl next)
                val value = here + overlap[last][next]
                if (value > saved[to][next]) saved[to][next] = value
            }
        }
        if (mask == full) Drill.write(mask, saved[mask].max())
    }
    return kept.sumOf { it.length } - saved[full].max()
}
