// kind: WRONG_BRANCH
// 체를 가장 큰 질의보다 하나 짧게 만든다. 가장 큰 수의 답이 비어 있다.
fun divisorCounts(queries: IntArray): IntArray {
    var limit = 1
    for (n in queries) if (n > limit) limit = n
    val counts = IntArray(limit + 1)
    for (d in 1 until limit) {
        var multiple = d
        while (multiple < limit) { counts[multiple] += 1; multiple += d }
    }
    return IntArray(queries.size) { i -> counts[queries[i]] }
}
