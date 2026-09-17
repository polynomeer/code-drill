// kind: WRONG_BRANCH
// 거리를 |dx| + |dy| 로 잰다. 축에 나란한 쌍에서만 맞는다.
fun closestPairSquared(points: IntArray): Int {
    val n = points.size / 2
    var best = Long.MAX_VALUE
    var bestSq = Long.MAX_VALUE
    for (i in 0 until n) for (j in i + 1 until n) {
        val dx = (points[i * 2] - points[j * 2]).toLong(); val dy = (points[i * 2 + 1] - points[j * 2 + 1]).toLong()
        val manhattan = kotlin.math.abs(dx) + kotlin.math.abs(dy)
        if (manhattan < best) { best = manhattan; bestSq = dx * dx + dy * dy }
    }
    return bestSq.toInt()
}
