// kind: MISSING_EDGE_CASE
// 다 꺼냈는지 보지 않는다. 순환이 있어도 답을 낸다.
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val dist = IntArray(n); val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[tail++] = v
    var best = 0
    while (head < tail) { val u = queue[head++]; if (dist[u] > best) best = dist[u]; for (v in adj[u]) { if (dist[u] + 1 > dist[v]) dist[v] = dist[u] + 1; indegree[v] -= 1; if (indegree[v] == 0) queue[tail++] = v } }
    return best
}
