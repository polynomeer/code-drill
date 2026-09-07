// kind: MISSING_EDGE_CASE
// 이미 칠해진 이웃과 편이 같은지 보지 않는다.
fun isBipartite(n: Int, edges: IntArray): Int {
    val graph = Array(n) { mutableListOf<Int>() }
    var i = 0
    while (i < edges.size) {
        graph[edges[i]].add(edges[i + 1])
        graph[edges[i + 1]].add(edges[i])
        i += 2
    }
    val side = IntArray(n)
    for (root in 0 until n) {
        if (side[root] != 0) continue
        side[root] = 1
        val queue = ArrayDeque<Int>()
        queue.addLast(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (next in graph[node]) {
                if (side[next] == 0) { side[next] = -side[node]; queue.addLast(next) }
            }
        }
    }
    return 1
}
