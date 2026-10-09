// kind: PERFORMANCE
// 벨만–포드로 간선 목록을 바뀌지 않을 때까지 훑는다. 맞지만, 섞인 긴 사슬에서는 정점 수만큼 바퀴를 돈다.
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val dist = LongArray(n) { Long.MAX_VALUE }
    dist[source] = 0
    var changed = true
    var rounds = 0
    while (changed && rounds < n) {
        changed = false
        rounds += 1
        for (e in 0 until edges.size / 3) {
            val u = edges[3 * e]; val v = edges[3 * e + 1]
            if (dist[u] != Long.MAX_VALUE && dist[u] + edges[3 * e + 2] < dist[v]) { dist[v] = dist[u] + edges[3 * e + 2]; changed = true }
        }
    }
    return IntArray(n) { if (dist[it] == Long.MAX_VALUE) Int.MAX_VALUE else dist[it].toInt() }
}
