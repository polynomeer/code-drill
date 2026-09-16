// 검증용 정답 (§6.1 solutions/). 방향 간선으로 다익스트라, 답은 최댓값.
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(m)
    val to = IntArray(m)
    val cost = IntArray(m)
    var e = 0
    var i = 0
    while (i < edges.size) {
        to[e] = edges[i + 1]; cost[e] = edges[i + 2]; next[e] = head[edges[i]]; head[edges[i]] = e; e += 1
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
    var answer = 0
    for (v in 0 until n) {
        if (dist[v] == -1) return -1
        if (dist[v] > answer) answer = dist[v]
    }
    return answer
}
