// 검증용 정답 (§6.1 solutions/). √base 까지 소인수분해하고, 소인수마다 르장드르 공식.
fun trailingZerosInBase(n: Int, base: Int): Int {
    fun legendre(p: Int): Int {
        var q = n
        var count = 0
        while (q > 0) { q /= p; count += q }
        return count
    }
    var b = base
    var best = Int.MAX_VALUE
    var d = 2
    while (d.toLong() * d <= b) {
        if (b % d == 0) {
            var e = 0
            while (b % d == 0) { b /= d; e += 1 }
            val times = legendre(d) / e
            Drill.write(d, times)
            best = minOf(best, times)
        }
        d += 1
    }
    if (b > 1) best = minOf(best, legendre(b))
    return best
}
