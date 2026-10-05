// kind: WRONG_BRANCH
// 가장 짧은 길과 길이가 같은 다른 길을 둘째로 센다. 둘째는 엄격히 길어야 한다.
fun secondShortestPath(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2])); adj[edges[i + 1]].add(intArrayOf(edges[i], edges[i + 2])) }
    val inf = Long.MAX_VALUE / 4
    val first = LongArray(n) { inf }; val second = LongArray(n) { inf }
    first[0] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0, 0))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val d = top[0]; val v = top[1].toInt()
        if (d > second[v]) continue
        for (e in adj[v]) {
            val u = e[0]; var candidate = d + e[1]
            if (candidate < first[u]) { val pushed = first[u]; first[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())); candidate = pushed }
            else if (candidate == first[u] && second[u] > candidate) { second[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())); continue }
            if (candidate > first[u] && candidate < second[u]) { second[u] = candidate; heap.add(longArrayOf(candidate, u.toLong())) }
        }
    }
    return if (second[n - 1] >= inf) -1 else second[n - 1].toInt()
}
