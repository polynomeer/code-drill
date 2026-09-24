// 검증용 정답 (§6.1 solutions/). 약수를 고정하고 그 배수를 훑는다 — 전부 합쳐 n log n.
fun divisorCounts(queries: IntArray): IntArray {
    var limit = 1
    for (n in queries) if (n > limit) limit = n
    val counts = IntArray(limit + 1)
    for (d in 1..limit) {
        var multiple = d
        while (multiple <= limit) { counts[multiple] += 1; multiple += d }
    }
    return IntArray(queries.size) { i -> counts[queries[i]].also { Drill.write(queries[i], it) } }
}
