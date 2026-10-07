// kind: WRONG_BRANCH
// 소인수의 지수로 나누지 않는다. base 가 p 의 거듭제곱을 품으면 p 가 그만큼 더 필요하다.
fun trailingZerosInBase(n: Int, base: Int): Int {
    var b = base; var best = Int.MAX_VALUE
    var d = 2
    fun legendre(p: Int): Int { var q = n; var c = 0; while (q > 0) { q /= p; c += q }; return c }
    while (d.toLong() * d <= b) {
        if (b % d == 0) { while (b % d == 0) b /= d; best = minOf(best, legendre(d)) }
        d += 1
    }
    if (b > 1) best = minOf(best, legendre(b))
    return best
}
