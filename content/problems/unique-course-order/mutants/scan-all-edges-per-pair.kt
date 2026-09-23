// kind: PERFORMANCE
// 위상 순서를 하나 구한 뒤 이웃한 두 과목마다 간선 배열을 처음부터 훑는다. O(n*m).
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val order = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) order[back++] = v
    while (front < back) {
        val u = order[front++]
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) order[back++] = v }
    }
    if (back != n) return 0
    for (i in 0 until n - 1) {
        var linked = false
        for (j in edges.indices step 2) {
            Drill.compare(order[i], order[i + 1])
            if (edges[j] == order[i] && edges[j + 1] == order[i + 1]) { linked = true; break }
        }
        if (!linked) return 0
    }
    return 1
}
