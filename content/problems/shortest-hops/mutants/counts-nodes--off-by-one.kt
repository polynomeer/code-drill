// kind: OFF_BY_ONE
// 간선 수가 아니라 지나온 정점 수를 센다.
fun shortestHops(n: Int, edges: IntArray, target: Int): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val distance = IntArray(n) { -1 }
    distance[0] = 1
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
