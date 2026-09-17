// 검증용 정답 (§6.1 solutions/). x 정렬 → 분할 → 띠에서 y 순으로 이웃만.
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    val order = (0 until n).sortedWith(compareBy({ points[it * 2] }, { points[it * 2 + 1] })).toIntArray()
    val xs = IntArray(n) { points[order[it] * 2] }
    val ys = IntArray(n) { points[order[it] * 2 + 1] }
    fun dist(i: Int, j: Int): Long {
        val dx = (xs[i] - xs[j]).toLong(); val dy = (ys[i] - ys[j]).toLong()
        return dx * dx + dy * dy
    }
    val strip = IntArray(n)
    fun solve(lo: Int, hi: Int): Long {
        if (hi - lo <= 3) {
            var best = Long.MAX_VALUE
            for (i in lo until hi) for (j in i + 1 until hi) { Drill.compare(i, j); best = minOf(best, dist(i, j)) }
            return best
        }
        val mid = (lo + hi) / 2
        val midX = xs[mid]
        var best = minOf(solve(lo, mid), solve(mid, hi))
        var m = 0
        for (i in lo until hi) {
            val dx = (xs[i] - midX).toLong()
            if (dx * dx < best) strip[m++] = i
        }
        val band = strip.copyOfRange(0, m).sortedBy { ys[it] }
        for (a in band.indices) {
            var b = a + 1
            while (b < band.size) {
                val dy = (ys[band[b]] - ys[band[a]]).toLong()
                if (dy * dy >= best) break
                Drill.compare(band[a], band[b])
                val d = dist(band[a], band[b])
                if (d < best) { best = d; Drill.write(0, best.toInt()) }
                b += 1
            }
        }
        return best
    }
    return solve(0, n).toInt()
}
