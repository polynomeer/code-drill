// kind: WRONG_BRANCH
// 간선을 양방향으로 넣어 방향 하나짜리 간선도 순환으로 본다.
fun hasCycle(n: Int, edges: IntArray): Int {
    val inDegree = IntArray(n)
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        inDegree[edges[i + 1]] += 1
        inDegree[edges[i]] += 1
        i += 2
    }
    val queue = ArrayDeque<Int>()
    for (v in 0 until n) if (inDegree[v] == 0) queue.addLast(v)
    var done = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        done += 1
        for (next in graph[node]) {
            inDegree[next] -= 1
            if (inDegree[next] == 0) queue.addLast(next)
        }
    }
    return if (done == n) 0 else 1
}
