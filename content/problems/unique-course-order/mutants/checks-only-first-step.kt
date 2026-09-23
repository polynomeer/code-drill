// kind: WRONG_BRANCH
// 시작할 수 있는 과목이 하나인지만 보고 그 뒤로 갈라지는 자리는 보지 않는다.
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    var roots = 0
    for (v in 0 until n) if (indegree[v] == 0) roots += 1
    if (roots != 1) return 0
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[back++] = v
    var seen = 0
    while (front < back) {
        val u = queue[front++]; seen += 1
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) queue[back++] = v }
    }
    return if (seen == n) 1 else 0
}
