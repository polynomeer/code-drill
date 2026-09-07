// kind: PERFORMANCE
// 지수만큼 곱한다. 작은 지수는 통과한다.
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007L
    var result = 1L
    for (step in 0 until exponent) result = result * base % mod
    return result.toInt()
}
