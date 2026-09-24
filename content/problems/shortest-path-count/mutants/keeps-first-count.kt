// kind: MISSING_EDGE_CASE
// 같은 거리로 다시 닿았을 때 가짓수를 더하지 않는다. 길이 같은 다른 길이 세어지지 않는다.
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val ways = LongArray(n)
    val done = BooleanArray(n)
    dist[0] = 0; ways[0] = 1
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) {
            val candidate = dist[v] + e[1]
            if (candidate < dist[e[0]]) { dist[e[0]] = candidate; ways[e[0]] = ways[v]; heap.add(longArrayOf(candidate, e[0].toLong())) }
        }
    }
    return (ways[n - 1] % mod).toInt()
}
