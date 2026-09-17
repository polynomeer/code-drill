// kind: WRONG_ALGORITHM
// 양쪽의 답만 보고 경계를 가로지르는 쌍을 보지 않는다.
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    val order = (0 until n).sortedBy { points[it * 2] }.toIntArray()
    val xs = IntArray(n) { points[order[it] * 2] }
    val ys = IntArray(n) { points[order[it] * 2 + 1] }
    fun dist(i: Int, j: Int): Long { val dx = (xs[i] - xs[j]).toLong(); val dy = (ys[i] - ys[j]).toLong(); return dx * dx + dy * dy }
    fun solve(lo: Int, hi: Int): Long {
        if (hi - lo <= 3) { var best = Long.MAX_VALUE; for (i in lo until hi) for (j in i + 1 until hi) best = minOf(best, dist(i, j)); return best }
        val mid = (lo + hi) / 2
        return minOf(solve(lo, mid), solve(mid, hi))
    }
    return solve(0, n).toInt()
}
