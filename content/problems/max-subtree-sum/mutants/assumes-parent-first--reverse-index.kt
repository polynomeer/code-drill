// kind: MISSING_EDGE_CASE
// 인덱스 역순이 곧 자식→부모 순서라고 믿는다. 루트가 뒤에 있으면 합이 덜 올라간다.
fun maxSubtreeSum(parent: IntArray, values: IntArray): Int {
    val total = values.copyOf()
    var best = Int.MIN_VALUE
    for (v in parent.indices.reversed()) {
        if (total[v] > best) best = total[v]
        if (parent[v] != -1) total[parent[v]] += total[v]
    }
    return best
}
