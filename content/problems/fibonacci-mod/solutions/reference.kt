// 검증용 정답 (§6.1 solutions/). 빠른 두 배 공식, 비트를 위에서부터.
fun fibonacciMod(n: Int): Int {
    val mod = 1_000_000_007L
    var a = 0L; var b = 1L   // F(k), F(k+1), k = 0
    for (bit in 30 downTo 0) {
        Drill.compare(bit, 0)
        val c = a * ((2 * b - a + mod) % mod) % mod     // F(2k)
        val d = (a * a + b * b) % mod                    // F(2k+1)
        if ((n shr bit) and 1 == 1) { a = d; b = (c + d) % mod } else { a = c; b = d }
        Drill.write(0, a.toInt())
    }
    return a.toInt()
}
