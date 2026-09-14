// kind: OFF_BY_ONE
// 제곱이 n 을 처음 넘는 값을 답한다. 완전제곱수가 아니면 하나 크다.
fun isqrt(n: Int): Int {
    var lo = 0
    var hi = 46341
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (mid.toLong() * mid < n) lo = mid + 1 else hi = mid
    }
    return lo
}
