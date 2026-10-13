// kind: WRONG_BRANCH
// 간선을 u → v 한쪽으로만 쓴다. 그래프는 방향이 없다.
fun widestPath(n: Int, edges: IntArray, s: Int, t: Int): Int {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in 0 until edges.size / 3) adj[edges[3 * e]].add(intArrayOf(edges[3 * e + 1], edges[3 * e + 2]))
    val best = IntArray(n) { -1 }; best[s] = Int.MAX_VALUE
    val heap = java.util.PriorityQueue<IntArray>(compareByDescending { it[0] }); heap.add(intArrayOf(Int.MAX_VALUE, s))
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val u = top[1]
        if (top[0] != best[u]) continue
        for (edge in adj[u]) { val w = minOf(best[u], edge[1]); if (w > best[edge[0]]) { best[edge[0]] = w; heap.add(intArrayOf(w, edge[0])) } }
    }
    return best[t]
}
