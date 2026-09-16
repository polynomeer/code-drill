// kind: MISSING_EDGE_CASE
// 순환이 있어도 들을 수 있었던 과목까지만 돌려준다. 순환이면 빈 배열이어야 한다.
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i]].add(prereqs[i + 1]); indegree[prereqs[i + 1]] += 1; i += 2 }
    val heap = java.util.PriorityQueue<Int>()
    for (v in 0 until n) if (indegree[v] == 0) heap.add(v)
    val order = mutableListOf<Int>()
    while (heap.isNotEmpty()) {
        val v = heap.poll()
        order.add(v)
        for (nxt in graph[v]) { indegree[nxt] -= 1; if (indegree[nxt] == 0) heap.add(nxt) }
    }
    return order.toIntArray()
}
