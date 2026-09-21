// kind: PERFORMANCE
// n 번 더한다. 20 억 번이다.
fun fibonacciMod(n: Int): Int {
    val mod = 1_000_000_007L
    var a = 0L; var b = 1L
    for (k in 0 until n) { Drill.compare(k, 0); val t = (a + b) % mod; a = b; b = t }
    return a.toInt()
}
