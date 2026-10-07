// kind: MISSING_EDGE_CASE
// p 의 거듭제곱을 Int 로 곱해 나간다. n 이 크면 넘쳐 음수가 된다.
fun trailingZerosInBase(n: Int, base: Int): Int {
    fun legendre(p: Int): Int { var pk = p; var c = 0; var guard = 0; while (pk in 1..n && guard < 64) { c += n / pk; pk *= p; guard += 1 }; return c }
    var b = base; var best = Int.MAX_VALUE
    var d = 2
    while (d.toLong() * d <= b) {
        if (b % d == 0) { var e = 0; while (b % d == 0) { b /= d; e += 1 }; best = minOf(best, legendre(d) / e) }
        d += 1
    }
    if (b > 1) best = minOf(best, legendre(b))
    return best
}
