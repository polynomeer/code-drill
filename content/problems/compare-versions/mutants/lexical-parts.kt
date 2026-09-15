// kind: WRONG_ALGORITHM
// 자리를 문자열로 비교한다. 10 이 9 보다 작다.
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.'); val ys = b.split('.')
    val n = maxOf(xs.size, ys.size)
    for (i in 0 until n) {
        val x = if (i < xs.size) xs[i] else "0"
        val y = if (i < ys.size) ys[i] else "0"
        val c = x.compareTo(y)
        if (c != 0) return if (c < 0) -1 else 1
    }
    return 0
}
