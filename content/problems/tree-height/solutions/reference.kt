// 검증용 정답 (§6.1 solutions/). 깊이를 메모하며 올라간다. 정점마다 한 번.
fun treeHeight(parent: IntArray): Int {
    val depth = IntArray(parent.size) { -1 }
    var best = 0
    for (start in parent.indices) {
        // 아직 모르는 조상까지 올라가며 경로를 쌓고, 내려오며 깊이를 채운다.
        val path = ArrayList<Int>()
        var v = start
        while (v != -1 && depth[v] == -1) {
            path.add(v)
            Drill.edge(v.toString(), parent[v].toString())
            v = parent[v]
        }
        var d = if (v == -1) -1 else depth[v]
        for (i in path.indices.reversed()) {
            d += 1
            depth[path[i]] = d
            Drill.node(path[i].toString())
        }
        if (depth[start] > best) best = depth[start]
    }
    return best
}
