// kind: MISSING_EDGE_CASE
// 순환을 보지 않는다. 처리하지 못한 과목이 남아도 갈라지지만 않았으면 1 이다.
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[back++] = v
    while (front < back) {
        if (back - front > 1) return 0
        val u = queue[front++]
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) queue[back++] = v }
    }
    return 1
}
