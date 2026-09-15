// kind: MISSING_EDGE_CASE
// 끝의 .0 을 한 번만 떼고 길이를 비교한다. 1.0.0 과 1 이 달라진다.
fun compareVersions(a: String, b: String): Int {
    val xs = a.removeSuffix(".0").split('.').map { it.toLong() }
    val ys = b.removeSuffix(".0").split('.').map { it.toLong() }
    for (i in 0 until minOf(xs.size, ys.size)) if (xs[i] != ys[i]) return if (xs[i] < ys[i]) -1 else 1
    return xs.size.compareTo(ys.size).coerceIn(-1, 1)
}
