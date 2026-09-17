// kind: MISSING_EDGE_CASE
// 띠에서 이웃을 y 차와 무관하게 다음 둘만 본다. 답인 두 점 사이에 둘이 끼면 놓친다.
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    val order = (0 until n).sortedWith(compareBy({ points[it * 2] }, { points[it * 2 + 1] })).toIntArray()
    val xs = IntArray(n) { points[order[it] * 2] }
    val ys = IntArray(n) { points[order[it] * 2 + 1] }
    fun dist(i: Int, j: Int): Long { val dx = (xs[i] - xs[j]).toLong(); val dy = (ys[i] - ys[j]).toLong(); return dx * dx + dy * dy }
    fun solve(lo: Int, hi: Int): Long {
        if (hi - lo <= 3) { var best = Long.MAX_VALUE; for (i in lo until hi) for (j in i + 1 until hi) best = minOf(best, dist(i, j)); return best }
        val mid = (lo + hi) / 2; val midX = xs[mid]
        var best = minOf(solve(lo, mid), solve(mid, hi))
        val band = (lo until hi).filter { val dx = (xs[it] - midX).toLong(); dx * dx < best }.sortedBy { ys[it] }
        for (a in band.indices) for (b in a + 1 until minOf(band.size, a + 3)) best = minOf(best, dist(band[a], band[b]))
        return best
    }
    return solve(0, n).toInt()
}
