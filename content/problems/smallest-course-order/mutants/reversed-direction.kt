// kind: WRONG_ALGORITHM
// 조건의 방향을 거꾸로 읽는다 — b 를 들은 뒤 a 를 듣는다.
fun smallestOrder(n: Int, prereqs: IntArray): IntArray {
    val graph = Array(n) { mutableListOf<Int>() }
    val indegree = IntArray(n)
    var i = 0
    while (i < prereqs.size) { graph[prereqs[i + 1]].add(prereqs[i]); indegree[prereqs[i]] += 1; i += 2 }
    val heap = java.util.PriorityQueue<Int>()
    for (v in 0 until n) if (indegree[v] == 0) heap.add(v)
    val order = mutableListOf<Int>()
    while (heap.isNotEmpty()) {
        val v = heap.poll()
        order.add(v)
        for (nxt in graph[v]) { indegree[nxt] -= 1; if (indegree[nxt] == 0) heap.add(nxt) }
    }
    return if (order.size == n) order.toIntArray() else IntArray(0)
}
