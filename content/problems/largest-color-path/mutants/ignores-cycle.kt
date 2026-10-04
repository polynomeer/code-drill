// kind: MISSING_EDGE_CASE
// 위상 정렬이 모든 정점을 꺼냈는지 보지 않는다. 순환이 있으면 -1 이어야 한다.
fun largestColorPath(colors: String, edges: IntArray): Int {
    val n = colors.length
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val best = Array(n) { IntArray(26) }
    val queue = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.add(v)
    var answer = 0
    while (queue.isNotEmpty()) {
        val v = queue.poll()
        best[v][colors[v] - 'a'] += 1
        answer = maxOf(answer, best[v][colors[v] - 'a'])
        for (u in adj[v]) {
            for (k in 0 until 26) best[u][k] = maxOf(best[u][k], best[v][k])
            indegree[u] -= 1
            if (indegree[u] == 0) queue.add(u)
        }
    }
    return answer
}
