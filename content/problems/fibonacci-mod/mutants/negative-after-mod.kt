// kind: MISSING_EDGE_CASE
// 2F(k+1) − F(k) 가 음수여도 그대로 곱한다. 음수 나머지가 답에 남는다.
fun fibonacciMod(n: Int): Int {
    val mod = 1_000_000_007L
    var a = 0L; var b = 1L
    for (bit in 30 downTo 0) {
        val c = a * ((2 * b - a) % mod) % mod
        val d = (a * a + b * b) % mod
        if ((n shr bit) and 1 == 1) { a = d; b = (c + d) % mod } else { a = c; b = d }
    }
    return a.toInt()
}
