// kind: OFF_BY_ONE
// 간선 수가 아니라 정점 수를 센다. 높이가 늘 하나 크다.
fun treeHeight(parent: IntArray): Int {
    val depth = IntArray(parent.size) { -1 }
    var best = 0
    for (start in parent.indices) {
        val path = ArrayList<Int>()
        var v = start
        while (v != -1 && depth[v] == -1) { path.add(v); v = parent[v] }
        var d = if (v == -1) 0 else depth[v]
        for (i in path.indices.reversed()) { d += 1; depth[path[i]] = d }
        if (depth[start] > best) best = depth[start]
    }
    return best
}
