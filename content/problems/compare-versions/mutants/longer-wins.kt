// kind: MISSING_EDGE_CASE
// 공통 자리가 같으면 자리가 많은 쪽이 크다고 본다. 1.0 과 1.0.0 이 달라진다.
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.').map { it.toLong() }; val ys = b.split('.').map { it.toLong() }
    for (i in 0 until minOf(xs.size, ys.size)) if (xs[i] != ys[i]) return if (xs[i] < ys[i]) -1 else 1
    return xs.size.compareTo(ys.size).coerceIn(-1, 1)
}
