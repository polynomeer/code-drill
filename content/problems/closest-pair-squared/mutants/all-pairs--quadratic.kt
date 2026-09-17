// kind: PERFORMANCE
// 모든 쌍을 본다. O(n²).
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    var best = Long.MAX_VALUE
    for (i in 0 until n) for (j in i + 1 until n) {
        Drill.compare(i, j)
        val dx = (points[i * 2] - points[j * 2]).toLong(); val dy = (points[i * 2 + 1] - points[j * 2 + 1]).toLong()
        val d = dx * dx + dy * dy
        if (d < best) best = d
    }
    return best.toInt()
}
