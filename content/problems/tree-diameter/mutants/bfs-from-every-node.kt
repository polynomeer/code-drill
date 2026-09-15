// kind: PERFORMANCE
// 모든 정점에서 BFS 로 가장 먼 정점을 찾는다. O(n²).
fun treeDiameter(parent: IntArray): Int {
    val n = parent.size
    val adj = Array(n) { mutableListOf<Int>() }
    for (v in 0 until n) if (parent[v] != -1) { adj[v].add(parent[v]); adj[parent[v]].add(v) }
    var best = 0
    val dist = IntArray(n)
    for (s in 0 until n) {
        java.util.Arrays.fill(dist, -1)
        dist[s] = 0
        val queue = ArrayDeque<Int>(); queue.addLast(s)
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            Drill.visit(v, dist[v])
            best = maxOf(best, dist[v])
            for (u in adj[v]) if (dist[u] == -1) { dist[u] = dist[v] + 1; queue.addLast(u) }
        }
    }
    return best
}
