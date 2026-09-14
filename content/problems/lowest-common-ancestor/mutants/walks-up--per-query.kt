// kind: PERFORMANCE
// 질의마다 한 칸씩 올라간다. 사슬에서 질의 × 깊이.
fun lowestCommonAncestors(parent: IntArray, queries: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    val path = IntArray(n)
    for (start in 0 until n) {
        var v = start; var len = 0
        while (v != -1 && depth[v] == -1) { path[len++] = v; v = parent[v] }
        var d = if (v == -1) -1 else depth[v]
        while (len > 0) { d += 1; depth[path[--len]] = d }
    }
    val out = IntArray(queries.size / 2)
    for (i in out.indices) {
        var a = queries[2 * i]; var b = queries[2 * i + 1]
        while (depth[a] > depth[b]) { Drill.compare(a, b); a = parent[a] }
        while (depth[b] > depth[a]) { Drill.compare(a, b); b = parent[b] }
        while (a != b) { Drill.compare(a, b); a = parent[a]; b = parent[b] }
        out[i] = a
    }
    return out
}
