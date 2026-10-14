// kind: WRONG_BRANCH
// 남은 걸음이 음수여도 그대로 더한다. 닿지 못한 쪽에서는 0 걸음이 들어간다.
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
    for (e in 0 until m) {
        val u = edges[3 * e]; val v = edges[3 * e + 1]; val c = edges[3 * e + 2]
        val fromU = if (dist[u] == Long.MAX_VALUE) 0L else maxMoves - dist[u]
        val fromV = if (dist[v] == Long.MAX_VALUE) 0L else maxMoves - dist[v]
        total += minOf(c.toLong(), fromU + fromV)
    }
    return total.toInt()
}
