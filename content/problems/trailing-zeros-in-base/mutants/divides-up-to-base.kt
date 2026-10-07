// kind: PERFORMANCE
// 나눠 볼 수를 √base 가 아니라 base 까지 늘린다. base 가 큰 소수면 10 억 번을 나눈다.
fun trailingZerosInBase(n: Int, base: Int): Int {
    fun legendre(p: Int): Int { var q = n; var c = 0; while (q > 0) { q /= p; c += q }; return c }
    var b = base; var best = Int.MAX_VALUE
    var d = 2
    while (d <= base && b > 1) {
        if (b % d == 0) { var e = 0; while (b % d == 0) { b /= d; e += 1 }; best = minOf(best, legendre(d) / e) }
        d += 1
    }
    return best
}
