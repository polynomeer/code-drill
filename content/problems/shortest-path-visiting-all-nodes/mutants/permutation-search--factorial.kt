// kind: PERFORMANCE
// 방문 순서 전부를 시도하며 쌍마다 최단 거리를 더한다. 가지치기가 없어 O(n! · n).
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val dist = Array(n) { IntArray(n) { -1 } }
    for (s in 0 until n) {
        val queue = ArrayDeque<Int>(); dist[s][s] = 0; queue.add(s)
        while (queue.isNotEmpty()) { val u = queue.removeFirst(); for (v in adj[u]) if (dist[s][v] < 0) { dist[s][v] = dist[s][u] + 1; queue.add(v) } }
    }
    var best = Int.MAX_VALUE
    val used = BooleanArray(n)
    fun go(last: Int, count: Int, total: Int) {
        if (count == n) { if (total < best) best = total; return }
        for (v in 0 until n) if (!used[v]) { Drill.compare(last, v); used[v] = true; go(v, count + 1, total + dist[last][v]); used[v] = false }
    }
    for (s in 0 until n) { used[s] = true; go(s, 1, 0); used[s] = false }
    return best
}
