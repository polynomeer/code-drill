// kind: PERFORMANCE
// 최단 거리를 구한 뒤 경로를 하나씩 세어 나간다. 갈래가 늘면 경로 수만큼 돈다.
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val dist = LongArray(n) { inf }
    val done = BooleanArray(n)
    dist[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) { val candidate = dist[v] + e[1]; if (candidate < dist[e[0]]) { dist[e[0]] = candidate; heap.add(longArrayOf(candidate, e[0].toLong())) } }
    }
    if (dist[n - 1] >= inf) return 0
    var total = 0L
    fun walk(v: Int, spent: Long) {
        Drill.compare(v, spent.toInt())
        if (v == n - 1) { total = (total + 1) % mod; return }
        for (e in adj[v]) {
            val next = spent + e[1]
            if (next + dist[n - 1] - dist[n - 1] <= dist[n - 1] && next == dist[e[0]]) walk(e[0], next)
        }
    }
    walk(0, 0)
    return (total % mod).toInt()
}
