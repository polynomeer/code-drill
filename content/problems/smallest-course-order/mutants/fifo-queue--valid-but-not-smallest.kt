// kind: WRONG_BRANCH
// 들을 수 있는 과목을 선입선출로 든다. 유효한 순서이지만 사전순이 아니다.
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (indegree[v] == 0) queue.addLast(v)
    val order = mutableListOf<Int>()
    while (queue.isNotEmpty()) {
        val v = queue.removeFirst()
        order.add(v)
        for (nxt in graph[v]) { indegree[nxt] -= 1; if (indegree[nxt] == 0) queue.addLast(nxt) }
    }
    return if (order.size == n) order.toIntArray() else IntArray(0)
}
