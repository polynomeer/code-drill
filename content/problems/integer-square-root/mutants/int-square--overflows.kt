// kind: MISSING_EDGE_CASE
// 가운데 값의 제곱을 Int 로 계산한다. 46341 부터 넘쳐 음수가 되고 조건이 뒤집힌다.
fun isqrt(n: Int): Int {
    var lo = 0
    var hi = n
    while (lo < hi) {
        val mid = (lo + hi + 1) / 2
        if (mid * mid <= n) lo = mid else hi = mid - 1
    }
    return lo
}
