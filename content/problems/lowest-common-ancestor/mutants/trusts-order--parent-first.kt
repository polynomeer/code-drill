// kind: MISSING_EDGE_CASE
// 부모가 자식보다 먼저 온다고 믿고 순서대로 깊이를 정한다. 루트가 뒤에 오면 깊이가 틀린다.
fun lowestCommonAncestors(parent: IntArray, queries: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n)
    for (v in 0 until n) depth[v] = if (parent[v] == -1) 0 else depth[parent[v]] + 1
    val out = IntArray(queries.size / 2)
    for (i in out.indices) {
        var a = queries[2 * i]; var b = queries[2 * i + 1]
        while (depth[a] > depth[b]) a = parent[a]
        while (depth[b] > depth[a]) b = parent[b]
        while (a != b) { a = parent[a]; b = parent[b] }
        out[i] = a
    }
    return out
}
