// kind: MISSING_EDGE_CASE
// 자기 자신을 약수로 세지 않는다. 모든 답이 하나씩 적다.
fun divisorCounts(queries: IntArray): IntArray {
    var limit = 1
    for (n in queries) if (n > limit) limit = n
    val counts = IntArray(limit + 1)
    for (d in 1..limit / 2) {
        var multiple = d
        while (multiple <= limit) { counts[multiple] += 1; multiple += d }
    }
    return IntArray(queries.size) { i -> counts[queries[i]] }
}
