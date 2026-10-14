// kind: WRONG_ALGORITHM
// 원래 정점만 센다. 간선 위의 새 정점도 닿으면 센다.
fun subdividedReach(n: Int, edges: IntArray, maxMoves: Int): Int {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }; val next = IntArray(2 * m); val to = IntArray(2 * m); val weight = IntArray(2 * m)
    for (e in 0 until m) {
        val u = edges[3 * e]; val v = edges[3 * e + 1]; val w = edges[3 * e + 2] + 1
        to[2 * e] = v; weight[2 * e] = w; next[2 * e] = head[u]; head[u] = 2 * e
        to[2 * e + 1] = u; weight[2 * e + 1] = w; next[2 * e + 1] = head[v]; head[v] = 2 * e + 1
    }
    val dist = LongArray(n) { Long.MAX_VALUE }
    dist[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val u = top[1].toInt()
        if (top[0] != dist[u]) continue
        Drill.visit(u, minOf(dist[u], Int.MAX_VALUE.toLong()).toInt())
        var e = head[u]
        while (e != -1) {
            val d = dist[u] + weight[e]
            if (d < dist[to[e]]) { dist[to[e]] = d; heap.add(longArrayOf(d, to[e].toLong())) }
            e = next[e]
        }
    }
    var total = 0L
    for (v in 0 until n) if (dist[v] <= maxMoves) total += 1
    return total.toInt()
}
