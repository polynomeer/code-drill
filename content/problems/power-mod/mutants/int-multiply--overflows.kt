// kind: MISSING_EDGE_CASE
// Int 로 곱해 중간값이 넘친다.
fun powerMod(base: Int, exponent: Int): Int {
    val mod = 1_000_000_007
    var result = 1
    var current = base % mod
    var remaining = exponent
    while (remaining > 0) {
        if (remaining and 1 == 1) result = result * current % mod
        current = current * current % mod
        remaining = remaining shr 1
    }
    return result
}
