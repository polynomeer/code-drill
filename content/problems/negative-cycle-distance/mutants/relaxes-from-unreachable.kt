// kind: MISSING_EDGE_CASE
// 닿을 수 없는 정점에서도 완화한다. 무한대에 가중치를 더해 닿지 않는 곳에 거리가 생긴다.
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val dist = LongArray(n) { inf }
    dist[0] = 0L
    for (round in 0 until n - 1) {
        var changed = false
        for (i in edges.indices step 3) {
            val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
            if (dist[a] + w < dist[b]) { dist[b] = dist[a] + w; changed = true }
        }
        if (!changed) break
    }
    val tainted = BooleanArray(n)
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (dist[a] + w < dist[b]) tainted[b] = true
    }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 3) adj[edges[i]].add(edges[i + 1])
    val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (tainted[v]) queue[tail++] = v
    while (head < tail) { val u = queue[head++]; for (v in adj[u]) if (!tainted[v]) { tainted[v] = true; queue[tail++] = v } }
    return IntArray(n) { v -> if (tainted[v]) -1_000_000_000 else dist[v].toInt() }
}
