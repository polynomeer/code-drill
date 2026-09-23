// kind: MISSING_EDGE_CASE
// 고를 개수가 가진 개수보다 많은 질의를 따로 보지 않는다. 없는 자리를 읽는다.
fun binomialMod(queries: IntArray): IntArray {
    val mod = 1_000_000_007L
    var limit = 0
    for (i in queries.indices step 2) if (queries[i] > limit) limit = queries[i]
    val fact = LongArray(limit + 1)
    fact[0] = 1L
    for (i in 1..limit) fact[i] = fact[i - 1] * i % mod
    fun power(base: Long, exponent: Long): Long {
        var result = 1L; var b = base % mod; var e = exponent
        while (e > 0) { if (e and 1L == 1L) result = result * b % mod; b = b * b % mod; e = e shr 1 }
        return result
    }
    val inverse = LongArray(limit + 1)
    inverse[limit] = power(fact[limit], mod - 2)
    for (i in limit downTo 1) inverse[i - 1] = inverse[i] * i % mod
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val n = queries[2 * q]; val k = queries[2 * q + 1]
        out[q] = (fact[n] * inverse[k] % mod * inverse[n - k] % mod).toInt()
    }
    return out
}
