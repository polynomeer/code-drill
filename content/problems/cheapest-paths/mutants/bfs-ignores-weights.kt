// kind: WRONG_ALGORITHM
// 비용을 보지 않고 간선 수로 고른다. 간선은 적지만 비싼 길을 답으로 삼는다.
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1] to edges[i + 2]); graph[edges[i + 1]].add(edges[i] to edges[i + 2]); i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(source)
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] != -1) continue
            dist[nxt] = dist[node] + w
            queue.addLast(nxt)
        }
    }
    return dist
}
