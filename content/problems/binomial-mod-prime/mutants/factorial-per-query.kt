// kind: PERFORMANCE
// 질의마다 팩토리얼을 처음부터 다시 곱한다. 질의 수 × n.
fun binomialMod(queries: IntArray): IntArray {
    val mod = 1_000_000_007L
    fun power(base: Long, exponent: Long): Long {
        var result = 1L; var b = base % mod; var e = exponent
        while (e > 0) { if (e and 1L == 1L) result = result * b % mod; b = b * b % mod; e = e shr 1 }
        return result
    }
    val out = IntArray(queries.size / 2)
    for (q in out.indices) {
        val n = queries[2 * q]; val k = queries[2 * q + 1]
        if (k > n) { out[q] = 0; continue }
        var top = 1L
        for (i in 1..n) { top = top * i % mod; Drill.compare(q, i) }
        var bottom = 1L
        for (i in 1..k) bottom = bottom * i % mod
        for (i in 1..n - k) bottom = bottom * i % mod
        out[q] = (top * power(bottom, mod - 2) % mod).toInt()
    }
    return out
}
