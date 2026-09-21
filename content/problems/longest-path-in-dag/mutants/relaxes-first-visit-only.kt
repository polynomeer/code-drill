// kind: WRONG_BRANCH
// 정점의 거리를 처음 닿을 때만 정한다. 더 긴 경로가 나중에 와도 갱신하지 않는다.
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val dist = IntArray(n) { -1 }; val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) { queue[tail++] = v; dist[v] = 0 }
    var best = 0
    while (head < tail) { val u = queue[head++]; if (dist[u] > best) best = dist[u]; for (v in adj[u]) { if (dist[v] < 0) dist[v] = dist[u] + 1; indegree[v] -= 1; if (indegree[v] == 0) queue[tail++] = v } }
    return if (tail == n) best else -1
}
