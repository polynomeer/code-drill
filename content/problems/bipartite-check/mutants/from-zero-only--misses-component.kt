// kind: MISSING_EDGE_CASE
// 0 에서만 칠해 다른 덩어리의 홀수 순환을 놓친다.
fun isBipartite(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val side = IntArray(n)
    side[0] = 1
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for (next in graph[node]) {
            if (side[next] == 0) { side[next] = -side[node]; queue.addLast(next) }
            else if (side[next] == side[node]) return 0
        }
    }
    return 1
}
