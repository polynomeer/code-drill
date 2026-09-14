// kind: MISSING_EDGE_CASE
// 부모가 자식보다 먼저 온다고 믿고 순서대로 깊이를 정한다. 루트가 뒤에 오면 틀린다.
fun levelSums(parent: IntArray, values: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n)
    for (v in 0 until n) depth[v] = if (parent[v] == -1) 0 else depth[parent[v]] + 1
    val sums = IntArray(depth.max() + 1)
    for (v in 0 until n) sums[depth[v]] += values[v]
    return sums
}
