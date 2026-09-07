// kind: MISSING_EDGE_CASE
// 지수가 0 일 때 1 이 아니라 0 을 돌려준다.
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007L
    if (exponent == 0) return 0
    var result = 1L
    var current = base.toLong() % mod
    var remaining = exponent
    while (remaining > 0) {
        if (remaining and 1 == 1) result = result * current % mod
        current = current * current % mod
        remaining = remaining shr 1
    }
    return result.toInt()
}
