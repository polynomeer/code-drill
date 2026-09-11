// kind: PERFORMANCE
// 정점마다 루트까지 다시 올라간다. 사슬에서 O(n²).
fun treeHeight(parent: IntArray): Int {
    var best = 0
    for (start in parent.indices) {
        var d = 0
        var v = start
        while (parent[v] != -1) { Drill.edge(v.toString(), parent[v].toString()); v = parent[v]; d += 1 }
        if (d > best) best = d
    }
    return best
}
