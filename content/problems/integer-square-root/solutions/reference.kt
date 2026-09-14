// 검증용 정답 (§6.1 solutions/). 답에 대한 이분 탐색, 제곱은 Long 으로.
fun isqrt(n: Int): Int {
    var lo = 0
    var hi = minOf(n.toLong(), 46340L).toInt()
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        Drill.compare(lo, hi)
        Drill.visit(mid, 0)
        if (mid.toLong() * mid <= n) lo = mid else hi = mid - 1
    }
    return lo
}
