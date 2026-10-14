// kind: PERFORMANCE
// 들은 집합을 기억하지 않고 고를 수 있는 모든 순서를 따라간다. 맞지만, 갈래가 곱으로 늘어난다.
fun coursesWithLimit(n: Int, edges: IntArray, k: Int): Int {
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    val full = (1 shl n) - 1
    fun go(mask: Int): Int {
        if (mask == full) return 0
        var ready = 0
        for (v in 0 until n) if (mask shr v and 1 == 0 && need[v] and mask == need[v]) ready = ready or (1 shl v)
        if (Integer.bitCount(ready) <= k) return 1 + go(mask or ready)
        var best = Int.MAX_VALUE
        var sub = ready
        while (sub != 0) {
            if (Integer.bitCount(sub) == k) best = minOf(best, 1 + go(mask or sub))
            sub = (sub - 1) and ready
        }
        return best
    }
    return go(0)
}
