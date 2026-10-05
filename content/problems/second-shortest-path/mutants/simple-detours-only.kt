// kind: WRONG_ALGORITHM
// 가장 짧은 길의 간선을 하나씩 빼고 다시 구한다. 같은 간선을 되돌아오는 길을 놓친다.
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val inf = Long.MAX_VALUE / 4
    fun dijkstra(banned: Int, parent: IntArray?): LongArray {
        val adj = Array(n) { ArrayList<IntArray>() }
        for (e in 0 until m) if (e != banned) { adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2], e)); adj[edges[3 * e + 1]].add(intArrayOf(edges[3 * e], edges[3 * e + 2], e)) }
        val dist = LongArray(n) { inf }
        dist[0] = 0
        val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
        heap.add(longArrayOf(0, 0))
        while (heap.isNotEmpty()) {
            val top = heap.poll(); val v = top[1].toInt()
            if (top[0] > dist[v]) continue
            for (e in adj[v]) if (dist[v] + e[1] < dist[e[0]]) { dist[e[0]] = dist[v] + e[1]; parent?.set(e[0], e[2]); heap.add(longArrayOf(dist[e[0]], e[0].toLong())) }
        }
        return dist
    }
    val parent = IntArray(n) { -1 }
    val base = dijkstra(-1, parent)
    if (base[n - 1] >= inf) return -1
    var best = inf
    var v = n - 1
    while (v != 0) {
        val e = parent[v]
        val d = dijkstra(e, null)[n - 1]
        if (d > base[n - 1] && d < best) best = d
        v = if (edges[3 * e] == v) edges[3 * e + 1] else edges[3 * e]
    }
    return if (best >= inf) -1 else best.toInt()
}
