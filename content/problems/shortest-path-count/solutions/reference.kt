// 검증용 정답 (§6.1 solutions/). 다익스트라로 거리를 확정하며 가짓수를 함께 옮긴다.
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val head = IntArray(n) { -1 }
    val next = IntArray(edges.size / 3 * 2)
    val to = IntArray(edges.size / 3 * 2)
    val weight = IntArray(edges.size / 3 * 2)
    var count = 0
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        to[count] = b; weight[count] = w; next[count] = head[a]; head[a] = count; count += 1
        to[count] = a; weight[count] = w; next[count] = head[b]; head[b] = count; count += 1
    }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val ways = LongArray(n)
    val done = BooleanArray(n)
    dist[0] = 0
    ways[0] = 1
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        Drill.visit(v, dist[v].toInt())
        var e = head[v]
        while (e != -1) {
            val u = to[e]
            val candidate = dist[v] + weight[e]
            if (candidate < dist[u]) {
                dist[u] = candidate
                ways[u] = ways[v]
                heap.add(longArrayOf(candidate, u.toLong()))
                Drill.write(u, ways[u].toInt())
            } else if (candidate == dist[u]) {
                ways[u] = (ways[u] + ways[v]) % mod
                Drill.write(u, ways[u].toInt())
            }
            e = next[e]
        }
    }
    return (ways[n - 1] % mod).toInt()
}
