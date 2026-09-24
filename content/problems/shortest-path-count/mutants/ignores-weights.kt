// kind: WRONG_BRANCH
// 간선을 한 걸음으로 세고 너비 우선으로 센다. 가중치가 다르면 최단이 아니다.
fun shortestPathCount(n: Int, edges: IntArray): Int {
    val mod = 1_000_000_007L
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 3) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val dist = IntArray(n) { -1 }
    val ways = LongArray(n)
    dist[0] = 0; ways[0] = 1
    val queue = java.util.ArrayDeque<Int>()
    queue.add(0)
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        for (u in adj[v]) {
            if (dist[u] == -1) { dist[u] = dist[v] + 1; ways[u] = ways[v]; queue.add(u) }
            else if (dist[u] == dist[v] + 1) ways[u] = (ways[u] + ways[v]) % mod
        }
    }
    return (ways[n - 1] % mod).toInt()
}
