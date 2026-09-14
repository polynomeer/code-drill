// kind: WRONG_ALGORITHM
// 과목을 하나씩 꺼내며 학기를 센다. 같은 학기에 들을 수 있는 과목을 따로 센다.
fun minSemesters(n: Int, prereqs: IntArray): Int {
    val indegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    var taken = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        taken += 1
        for (after in graph[node]) { indegree[after] -= 1; if (indegree[after] == 0) queue.addLast(after) }
    }
    return if (taken == n) taken else -1
}
