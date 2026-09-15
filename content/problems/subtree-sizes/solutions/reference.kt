// 검증용 정답 (§6.1 solutions/). 깊은 정점부터 부모에 더한다 — 재귀 없이.
fun subtreeSizes(parent: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    val path = IntArray(n)
    for (start in 0 until n) {
        var v = start
        var len = 0
        while (v != -1 && depth[v] == -1) { path[len++] = v; v = parent[v] }
        var d = if (v == -1) -1 else depth[v]
        while (len > 0) { d += 1; depth[path[--len]] = d }
    }
    val order = (0 until n).sortedByDescending { depth[it] }
    val size = IntArray(n) { 1 }
    for (v in order) {
        Drill.node("v$v")
        if (parent[v] != -1) {
            size[parent[v]] += size[v]
            Drill.write(parent[v], size[parent[v]])
        }
    }
    return size
}
