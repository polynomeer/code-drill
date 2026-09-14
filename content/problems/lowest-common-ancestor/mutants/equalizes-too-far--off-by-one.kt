// kind: OFF_BY_ONE
// 깊이를 맞출 때 같아진 뒤에도 한 칸 더 올린다. 한쪽이 다른 쪽의 조상이면 지나친다.
fun lowestCommonAncestors(parent: IntArray, queries: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    fun depthOf(v: Int): Int { if (depth[v] == -1) depth[v] = if (parent[v] == -1) 0 else depthOf(parent[v]) + 1; return depth[v] }
    for (v in 0 until n) depthOf(v)
    val out = IntArray(queries.size / 2)
    for (i in out.indices) {
        var a = queries[2 * i]; var b = queries[2 * i + 1]
        while (depth[a] >= depth[b] && parent[a] != -1 && a != b) a = parent[a]
        while (depth[b] > depth[a]) b = parent[b]
        while (a != b) { a = parent[a]; b = parent[b] }
        out[i] = a
    }
    return out
}
