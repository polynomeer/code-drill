// 검증용 정답 (§6.1 solutions/). Kahn 순서로 DP, 다 꺼내지 못하면 순환.
fun longestPathInDag(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val dist = IntArray(n)
    val queue = IntArray(n)
    var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[tail++] = v
    var best = 0
    while (head < tail) {
        val u = queue[head++]
        if (dist[u] > best) best = dist[u]
        for (v in adj[u]) {
            Drill.compare(u, v)
            if (dist[u] + 1 > dist[v]) { dist[v] = dist[u] + 1; Drill.write(v, dist[v]) }
            indegree[v] -= 1
            if (indegree[v] == 0) queue[tail++] = v
        }
    }
    return if (tail == n) best else -1
}
