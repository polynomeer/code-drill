// kind: MISSING_EDGE_CASE
// 갈 수 없는 정점(거리 무한대)도 건너뛰지 않고 더한다. 무한대끼리 더하면 넘쳐 음수가 되고, 그것이 가장 작은 값으로 뽑힌다.
fun requiredPathsWeight(n: Int, edges: IntArray, a: Int, b: Int, dest: Int): Int {
    val m = edges.size / 3
    fun build(reverse: Boolean): Array<IntArray> {
        val head = IntArray(n) { -1 }; val next = IntArray(m); val to = IntArray(m); val weight = IntArray(m)
        for (e in 0 until m) {
            val u = if (reverse) edges[3 * e + 1] else edges[3 * e]
            val v = if (reverse) edges[3 * e] else edges[3 * e + 1]
            to[e] = v; weight[e] = edges[3 * e + 2]; next[e] = head[u]; head[u] = e
        }
        return arrayOf(head, next, to, weight)
    }
    fun dijkstra(graph: Array<IntArray>, start: Int): LongArray {
        val (head, next, to, weight) = graph
        val dist = LongArray(n) { Long.MAX_VALUE }
        dist[start] = 0
        val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
        heap.add(longArrayOf(0, start.toLong()))
        while (heap.isNotEmpty()) {
            val top = heap.poll(); val u = top[1].toInt()
            if (top[0] != dist[u]) continue
            var e = head[u]
            while (e != -1) {
                val v = to[e]; val d = dist[u] + weight[e]
                if (d < dist[v]) { dist[v] = d; heap.add(longArrayOf(d, v.toLong())) }
                e = next[e]
            }
        }
        return dist
    }
    val forward = build(false)
    val fromA = dijkstra(forward, a); val fromB = dijkstra(forward, b); val toDest = dijkstra(build(true), dest)
    var best = Long.MAX_VALUE
    for (v in 0 until n) {
        val total = fromA[v] + fromB[v] + toDest[v]
        if (total < best) { best = total; Drill.visit(v, total.toInt()) }
    }
    return if (best == Long.MAX_VALUE) -1 else best.toInt()
}
