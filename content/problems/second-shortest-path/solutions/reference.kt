// 검증용 정답 (§6.1 solutions/). 정점마다 거리 둘을 들고 다니는 다익스트라.
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val head = IntArray(n) { -1 }
    val next = IntArray(2 * m)
    val to = IntArray(2 * m)
    val weight = IntArray(2 * m)
    var count = 0
    for (e in 0 until m) {
        val a = edges[3 * e]; val b = edges[3 * e + 1]; val w = edges[3 * e + 2]
        to[count] = b; weight[count] = w; next[count] = head[a]; head[a] = count; count += 1
        to[count] = a; weight[count] = w; next[count] = head[b]; head[b] = count; count += 1
    }
    val inf = Long.MAX_VALUE / 4
    val first = LongArray(n) { inf }
    val second = LongArray(n) { inf }
    first[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0]
        val v = top[1].toInt()
        if (d > second[v]) continue
        var e = head[v]
        while (e != -1) {
            val u = to[e]
            var candidate = d + weight[e]
            if (candidate < first[u]) {
                val pushed = first[u]
                first[u] = candidate
                heap.add(longArrayOf(candidate, u.toLong()))
                Drill.write(u, candidate.toInt())
                candidate = pushed
            }
            if (candidate > first[u] && candidate < second[u]) {
                second[u] = candidate
                heap.add(longArrayOf(candidate, u.toLong()))
                Drill.write(u, candidate.toInt())
            }
            e = next[e]
        }
    }
    return if (second[n - 1] >= inf) -1 else second[n - 1].toInt()
}
