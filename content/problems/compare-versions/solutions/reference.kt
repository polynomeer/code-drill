// 검증용 정답 (§6.1 solutions/). 자리마다 정수로, 빠진 자리는 0.
fun compareVersions(a: String, b: String): Int {
    val xs = a.split('.')
    val ys = b.split('.')
    val n = maxOf(xs.size, ys.size)
    for (i in 0 until n) {
        val x = if (i < xs.size) xs[i].toLong() else 0L
        val y = if (i < ys.size) ys[i].toLong() else 0L
        Drill.compare(i, i)
        if (x != y) return if (x < y) -1 else 1
    }
    return 0
}
