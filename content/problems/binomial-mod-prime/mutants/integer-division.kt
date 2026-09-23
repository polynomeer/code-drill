// kind: WRONG_ALGORITHM
// 나머지를 취한 값끼리 나눈다. 나머지 세계에는 나눗셈이 없어 나머지가 한 번 접히는 순간 어긋난다.
fun binomialMod(queries: IntArray): IntArray {
    val mod = 1_000_000_007L
    var limit = 0
    for (i in queries.indices step 2) if (queries[i] > limit) limit = queries[i]
    val fact = LongArray(limit + 1)
    fact[0] = 1L
    for (i in 1..limit) fact[i] = fact[i - 1] * i % mod
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val n = queries[2 * q]; val k = queries[2 * q + 1]
        out[q] = if (k > n) 0 else (fact[n] / (fact[k] * fact[n - k] % mod) % mod).toInt()
    }
    return out
}
