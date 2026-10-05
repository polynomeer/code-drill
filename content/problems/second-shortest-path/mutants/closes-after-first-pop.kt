// kind: WRONG_BRANCH
// 정점을 한 번 꺼내면 닫는다. 둘째 거리는 그 정점을 두 번째로 꺼낼 때 퍼진다.
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val first = LongArray(n) { inf }; val second = LongArray(n) { inf }
    val done = BooleanArray(n)
    first[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val d = top[0]; val v = top[1].toInt()
        if (done[v]) continue
        done[v] = true
        for (e in adj[v]) {
            val u = e[0]; var candidate = d + e[1]
            if (candidate < first[u]) { val pushed = first[u]; first[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())); candidate = pushed }
            if (candidate > first[u] && candidate < second[u]) second[u] = candidate
            val alt = second[v] + e[1]
            if (alt > first[u] && alt < second[u]) second[u] = alt
        }
    }
    return if (second[n - 1] >= inf) -1 else second[n - 1].toInt()
}
