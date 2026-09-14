// kind: MISSING_EDGE_CASE
// 못 들은 과목이 남아도 학기 수를 돌려준다. 순환을 알아채지 못한다.
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    var semesters = 0
    while (queue.isNotEmpty()) {
        semesters += 1
        repeat(queue.size) {
            val node = queue.removeFirst()
            for (after in graph[node]) { indegree[after] -= 1; if (indegree[after] == 0) queue.addLast(after) }
        }
    }
    return semesters
}
