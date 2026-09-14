// kind: WRONG_BRANCH
// 처음 적은 비용을 다시 낮추지 않는다. 더 싼 길이 나중에 발견되면 틀린다.
fun cheapestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1] to edges[i + 2]); graph[edges[i + 1]].add(edges[i] to edges[i + 2]); i += 3
    }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val node = top[1].toInt()
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] != -1) continue
            dist[nxt] = dist[node] + w
            heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong()))
        }
    }
    return dist
}
