// kind: WRONG_ALGORITHM
// 길이(폭의 합)가 가장 짧은 길을 찾고 그 길의 폭을 낸다. 짧은 길이 넓은 길은 아니다.
fun widestPath(n: Int, edges: IntArray, s: Int, t: Int): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in 0 until edges.size / 3) { adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2])); adj[edges[3 * e + 1]].add(intArrayOf(edges[3 * e], edges[3 * e + 2])) }
    val dist = LongArray(n) { Long.MAX_VALUE }; val width = IntArray(n)
    dist[s] = 0; width[s] = Int.MAX_VALUE
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] }); heap.add(longArrayOf(0, s.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val u = top[1].toInt()
        if (top[0] != dist[u]) continue
        for (edge in adj[u]) {
            val v = edge[0]; val d = dist[u] + edge[1]
            if (d < dist[v]) { dist[v] = d; width[v] = minOf(width[u], edge[1]); heap.add(longArrayOf(d, v.toLong())) }
        }
    }
    return if (dist[t] == Long.MAX_VALUE) -1 else width[t]
}
