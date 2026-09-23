// kind: WRONG_ALGORITHM
// 가장 가까운 정점부터 확정한다. 음수 간선이 나중에 더 싼 길을 만들면 놓친다.
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in edges.indices step 3) adj[edges[i]].add(intArrayOf(edges[i + 1], edges[i + 2]))
    val dist = IntArray(n) { inf }
    dist[0] = 0
    val done = BooleanArray(n)
    repeat(n) {
        var best = -1
        for (v in 0 until n) if (!done[v] && dist[v] != inf && (best == -1 || dist[v] < dist[best])) best = v
        if (best == -1) return@repeat
        done[best] = true
        for (e in adj[best]) if (dist[best] + e[1] < dist[e[0]]) dist[e[0]] = dist[best] + e[1]
    }
    return dist
}
