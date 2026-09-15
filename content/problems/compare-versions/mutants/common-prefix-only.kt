// kind: MISSING_EDGE_CASE
// 공통 자리만 비교하고 나머지는 무시한다. 1.0 과 1.0.1 이 같아진다.
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.').map { it.toLong() }; val ys = b.split('.').map { it.toLong() }
    for (i in 0 until minOf(xs.size, ys.size)) if (xs[i] != ys[i]) return if (xs[i] < ys[i]) -1 else 1
    return 0
}
