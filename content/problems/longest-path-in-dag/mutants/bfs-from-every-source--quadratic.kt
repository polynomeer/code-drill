// kind: PERFORMANCE
// 진입 차수 0 인 정점마다 따로 가장 긴 경로를 훑는다. 사슬 + 여분 간선에서 O(n·m).
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    // 순환 검사는 Kahn 으로.
    val deg = indegree.copyOf(); val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (deg[v] == 0) queue[tail++] = v
    while (head < tail) { val u = queue[head++]; for (v in adj[u]) { deg[v] -= 1; if (deg[v] == 0) queue[tail++] = v } }
    if (tail != n) return -1
    var best = 0
    for (s in 0 until n) {
        if (indegree[s] != 0) continue
        val dist = IntArray(n) { -1 }; dist[s] = 0
        val q = ArrayDeque<Int>(); q.addLast(s)
        while (q.isNotEmpty()) { val u = q.removeFirst(); if (dist[u] > best) best = dist[u]; for (v in adj[u]) { Drill.compare(u, v); if (dist[u] + 1 > dist[v]) { dist[v] = dist[u] + 1; q.addLast(v) } } }
    }
    return best
}
