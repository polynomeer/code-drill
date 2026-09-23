// kind: WRONG_ALGORITHM
// 순서가 있는지만 보고 하나뿐인지는 보지 않는다. 갈라지는 자리가 있어도 1 이다.
fun uniqueCourseOrder(n: Int, edges: IntArray): Int {
    val adj = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val queue = IntArray(n); var front = 0; var back = 0
    for (v in 0 until n) if (indegree[v] == 0) queue[back++] = v
    var seen = 0
    while (front < back) {
        val u = queue[front++]; seen += 1
        for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) queue[back++] = v }
    }
    return if (seen == n) 1 else 0
}
