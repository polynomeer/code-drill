// kind: PERFORMANCE
// 질의마다 1 부터 n 까지 전부 나눠 본다. 질의 수 × n.
fun divisorCounts(queries: IntArray): IntArray {
    return IntArray(queries.size) { i ->
        val n = queries[i]
        var count = 0
        for (d in 1..n) { Drill.compare(i, d); if (n % d == 0) count += 1 }
        count
    }
}
