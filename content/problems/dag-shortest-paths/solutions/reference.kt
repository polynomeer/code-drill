// 검증용 정답 (§6.1 solutions/). Kahn 의 위상 정렬로 순서를 얻고, 그 순서대로 나가는 간선을 완화한다.
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val indegree = IntArray(n)
    for (e in 0 until m) { val u = edges[3 * e]; next[e] = head[u]; head[u] = e; indegree[edges[3 * e + 1]] += 1 }
    val order = IntArray(n)
    var size = 0
    for (v in 0 until n) if (indegree[v] == 0) { order[size] = v; size += 1 }
    var read = 0
    while (read < size) {
        val u = order[read]; read += 1
        var e = head[u]
        while (e != -1) {
            val v = edges[3 * e + 1]
            indegree[v] -= 1
            if (indegree[v] == 0) { order[size] = v; size += 1 }
            e = next[e]
        }
    }
    val unreachable = Long.MAX_VALUE
    val dist = LongArray(n) { unreachable }
    dist[source] = 0
    for (u in order) {
        if (dist[u] == unreachable) continue
        Drill.visit(u, dist[u].toInt())
        var e = head[u]
        while (e != -1) {
            val v = edges[3 * e + 1]
            val candidate = dist[u] + edges[3 * e + 2]
            if (candidate < dist[v]) { dist[v] = candidate; Drill.edge(u.toString(), v.toString()) }
            e = next[e]
        }
    }
    return IntArray(n) { if (dist[it] == unreachable) Int.MAX_VALUE else dist[it].toInt() }
}
