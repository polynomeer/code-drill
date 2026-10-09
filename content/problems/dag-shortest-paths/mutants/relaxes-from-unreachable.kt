// kind: MISSING_EDGE_CASE
// 갈 수 없는 정점에서 나가는 간선도 완화한다. 무한대에 음수를 더한 값이 거리로 들어간다.
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val m = edges.size / 3
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (e in 0 until m) { adj[edges[3 * e]].add(e); indegree[edges[3 * e + 1]] += 1 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    val order = ArrayList<Int>()
    while (queue.isNotEmpty()) {
        val u = queue.removeFirst(); order.add(u)
        for (e in adj[u]) { val v = edges[3 * e + 1]; indegree[v] -= 1; if (indegree[v] == 0) queue.addLast(v) }
    }
    val inf = Int.MAX_VALUE.toLong()
    val dist = LongArray(n) { inf }
    dist[source] = 0
    for (u in order) for (e in adj[u]) {
        val v = edges[3 * e + 1]
        dist[v] = minOf(dist[v], dist[u] + edges[3 * e + 2])
    }
    return IntArray(n) { dist[it].toInt() }
}
