// kind: WRONG_ALGORITHM
// Kahn 알고리즘으로 순서 하나를 만들며 매번 고를 수 있는 정점의 수를 곱한다. 무엇을 고르느냐에 따라 다음 선택지가 달라진다.
fun topologicalOrdersCount(n: Int, edges: IntArray): Int {
    val indegree = IntArray(n); val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); indegree[edges[i + 1]] += 1 }
    val ready = java.util.ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) ready.add(v)
    var total = 1L
    while (ready.isNotEmpty()) {
        total = total * ready.size % 1_000_000_007
        val v = ready.poll()
        for (u in adj[v]) { indegree[u] -= 1; if (indegree[u] == 0) ready.add(u) }
    }
    return total.toInt()
}
