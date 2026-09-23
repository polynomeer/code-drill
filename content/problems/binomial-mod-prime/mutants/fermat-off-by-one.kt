// kind: OFF_BY_ONE
// 역원의 지수를 p-1 로 쓴다. 그 거듭제곱은 언제나 1 이라 나누지 않은 값이 나온다.
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
    inverse[limit] = power(fact[limit], mod - 1)
    for (i in limit downTo 1) inverse[i - 1] = inverse[i] * i % mod
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val n = queries[2 * q]; val k = queries[2 * q + 1]
        out[q] = if (k > n) 0 else (fact[n] * inverse[k] % mod * inverse[n - k] % mod).toInt()
    }
    return out
}
