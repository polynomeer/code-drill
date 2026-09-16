// kind: MISSING_EDGE_CASE
// 자기 자신으로 가는 간선을 빼고 센다. 자기 순환은 순환이다.
fun safeNodes(n: Int, edges: IntArray): IntArray {
    val reverse = Array(n) { mutableListOf<Int>() }
    val outdegree = IntArray(n)
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        if (a != b) { reverse[b].add(a); outdegree[a] += 1 }
        i += 2
    }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (outdegree[v] == 0) queue.addLast(v)
    val safe = BooleanArray(n)
    while (queue.isNotEmpty()) {
        val v = queue.removeFirst()
        safe[v] = true
        for (prev in reverse[v]) { outdegree[prev] -= 1; if (outdegree[prev] == 0) queue.addLast(prev) }
    }
    return (0 until n).filter { safe[it] }.toIntArray()
}
