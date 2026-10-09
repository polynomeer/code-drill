// kind: WRONG_ALGORITHM
// 다익스트라로 푼다. 한 번 확정한 정점이 나중에 음수 간선으로 더 짧아질 수 있다.
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in 0 until edges.size / 3) adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2]))
    val dist = LongArray(n) { Long.MAX_VALUE }
    val done = BooleanArray(n)
    dist[source] = 0
    val queue = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    queue.add(longArrayOf(0, source.toLong()))
    while (queue.isNotEmpty()) {
        val top = queue.poll(); val u = top[1].toInt()
        if (done[u]) continue
        done[u] = true
        for (edge in adj[u]) {
            val v = edge[0]
            if (!done[v] && dist[u] + edge[1] < dist[v]) { dist[v] = dist[u] + edge[1]; queue.add(longArrayOf(dist[v], v.toLong())) }
        }
    }
    return IntArray(n) { if (dist[it] == Long.MAX_VALUE) Int.MAX_VALUE else dist[it].toInt() }
}
