// kind: PERFORMANCE
// 다익스트라에서 다음 정점을 거리 표를 다 훑어 고른다. 맞지만, 정점 10 만이면 한 번에 100 억 걸음이다.
fun requiredPathsWeight(n: Int, edges: IntArray, a: Int, b: Int, dest: Int): Int {
    val m = edges.size / 3
    fun dijkstra(reverse: Boolean, start: Int): LongArray {
        val adj = Array(n) { ArrayList<IntArray>() }
        for (e in 0 until m) {
            val u = if (reverse) edges[3 * e + 1] else edges[3 * e]
            val v = if (reverse) edges[3 * e] else edges[3 * e + 1]
            adj[u].add(intArrayOf(v, edges[3 * e + 2]))
        }
        val dist = LongArray(n) { Long.MAX_VALUE }
        val done = BooleanArray(n)
        dist[start] = 0
        repeat(n) {
            var u = -1
            for (v in 0 until n) if (!done[v] && dist[v] != Long.MAX_VALUE && (u == -1 || dist[v] < dist[u])) u = v
            if (u == -1) return dist
            done[u] = true
            for (edge in adj[u]) if (dist[u] + edge[1] < dist[edge[0]]) dist[edge[0]] = dist[u] + edge[1]
        }
        return dist
    }
    val fromA = dijkstra(false, a); val fromB = dijkstra(false, b); val toDest = dijkstra(true, dest)
    var best = Long.MAX_VALUE
    for (v in 0 until n) {
        if (fromA[v] == Long.MAX_VALUE || fromB[v] == Long.MAX_VALUE || toDest[v] == Long.MAX_VALUE) continue
        best = minOf(best, fromA[v] + fromB[v] + toDest[v])
    }
    return if (best == Long.MAX_VALUE) -1 else best.toInt()
}
