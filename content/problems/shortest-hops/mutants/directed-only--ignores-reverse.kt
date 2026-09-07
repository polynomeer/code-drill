// kind: MISSING_EDGE_CASE
// 간선을 한 방향으로만 넣는다. 무방향인데 되돌아가지 못한다.
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        i += 2
    }
    val distance = IntArray(n) { -1 }
    distance[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (next in graph[node]) {
            if (distance[next] != -1) continue
            distance[next] = distance[node] + 1
            queue.addLast(next)
        }
    }
    return distance[target]
}
