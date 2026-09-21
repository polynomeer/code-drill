// kind: OFF_BY_ONE
// F(n+1) 을 답한다.
fun fibonacciMod(n: Int): Int {
    val mod = 1_000_000_007L
    var a = 0L; var b = 1L
    for (bit in 30 downTo 0) {
        val c = a * ((2 * b - a + mod) % mod) % mod
        val d = (a * a + b * b) % mod
        if ((n shr bit) and 1 == 1) { a = d; b = (c + d) % mod } else { a = c; b = d }
    }
    return b.toInt()
}
