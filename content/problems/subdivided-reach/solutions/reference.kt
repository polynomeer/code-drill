// 검증용 정답 (§6.1 solutions/). 원래 정점끼리 무게 c + 1 의 다익스트라, 간선마다 양 끝에서 남은 걸음을 더하되 c 를 넘지 않는다.
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
        val fromU = if (dist[u] == Long.MAX_VALUE) 0L else maxOf(0L, maxMoves - dist[u])
        val fromV = if (dist[v] == Long.MAX_VALUE) 0L else maxOf(0L, maxMoves - dist[v])
        total += minOf(c.toLong(), fromU + fromV)
    }
    return total.toInt()
}
