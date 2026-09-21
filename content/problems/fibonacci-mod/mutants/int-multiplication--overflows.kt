// kind: WRONG_BRANCH
// 곱을 Int 로 한다. (10⁹)² 이 넘친다.
fun fibonacciMod(n: Int): Int {
    val mod = 1_000_000_007
    var a = 0; var b = 1
    for (bit in 30 downTo 0) {
        val c = (a.toLong() * ((2L * b - a + mod) % mod) % mod).toInt()
        val d = ((a * a) % mod + (b * b) % mod) % mod
        if ((n shr bit) and 1 == 1) { a = d; b = (c + d) % mod } else { a = c; b = d }
    }
    return a
}
