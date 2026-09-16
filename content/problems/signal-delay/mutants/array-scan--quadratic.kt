// kind: PERFORMANCE
// 힙 없이 매번 모든 정점을 훑어 가장 빠른 미확정 정점을 고른다. O(n²).
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1] to edges[i + 2]); i += 3 }
    val dist = IntArray(n) { -1 }
    val done = BooleanArray(n)
    dist[source] = 0
    repeat(n) {
        var best = -1
        for (v in 0 until n) {
            Drill.compare(v, best)
            if (!done[v] && dist[v] != -1 && (best == -1 || dist[v] < dist[best])) best = v
        }
        if (best == -1) return -1
        done[best] = true
        for ((nxt, w) in graph[best]) {
            if (dist[nxt] == -1 || dist[best] + w < dist[nxt]) dist[nxt] = dist[best] + w
        }
    }
    return dist.max()
}
