// 검증용 정답 (§6.1 solutions/). 힙으로 가장 싼 정점부터 확정한다.
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(2 * m)
    val to = IntArray(2 * m)
    val cost = IntArray(2 * m)
    var e = 0
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        to[e] = b; cost[e] = w; next[e] = head[a]; head[a] = e; e += 1
        to[e] = a; cost[e] = w; next[e] = head[b]; head[b] = e; e += 1
        i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt()
        val node = top[1].toInt()
        if (d > dist[node]) continue
        Drill.node("v$node")
        var edge = head[node]
        while (edge != -1) {
            val nxt = to[edge]
            val nd = d + cost[edge]
            if (dist[nxt] == -1 || nd < dist[nxt]) {
                dist[nxt] = nd
                Drill.edge("v$node", "v$nxt")
                Drill.write(nxt, nd)
                heap.add(longArrayOf(nd.toLong(), nxt.toLong()))
            }
            edge = next[edge]
        }
    }
    return dist
}
