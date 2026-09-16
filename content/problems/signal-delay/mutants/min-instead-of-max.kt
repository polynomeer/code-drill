// kind: WRONG_BRANCH
// 가장 늦게 받는 정점이 아니라 가장 먼저 받는 정점의 시간을 돌려준다.
fun signalDelay(n: Int, edges: IntArray, source: Int): Int {
    val graph = Array(n) { mutableListOf<Pair<Int, Int>>() }
    var i = 0
    while (i < edges.size) { graph[edges[i]].add(edges[i + 1] to edges[i + 2]); i += 3 }
    val dist = IntArray(n) { -1 }
    dist[source] = 0
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    heap.add(longArrayOf(0L, source.toLong()))
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val d = top[0].toInt(); val node = top[1].toInt()
        if (d > dist[node]) continue
        for ((nxt, w) in graph[node]) {
            if (dist[nxt] == -1 || d + w < dist[nxt]) { dist[nxt] = d + w; heap.add(longArrayOf(dist[nxt].toLong(), nxt.toLong())) }
        }
    }
    if (dist.any { it == -1 }) return -1
    var best = Int.MAX_VALUE
    for (v in 0 until n) if (v != source && dist[v] < best) best = dist[v]
    return if (best == Int.MAX_VALUE) 0 else best
}
