// kind: PERFORMANCE
// 매번 모든 과목을 훑어 들을 수 있는 가장 작은 것을 찾는다. O(n²).
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val taken = BooleanArray(n)
    val order = IntArray(n)
    for (step in 0 until n) {
        var chosen = -1
        for (v in 0 until n) {
            Drill.compare(v, chosen)
            if (!taken[v] && indegree[v] == 0) { chosen = v; break }
        }
        if (chosen == -1) return IntArray(0)
        taken[chosen] = true
        order[step] = chosen
        for (nxt in graph[chosen]) indegree[nxt] -= 1
    }
    return order
}
