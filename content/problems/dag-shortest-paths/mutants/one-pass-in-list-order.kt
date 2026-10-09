// kind: WRONG_BRANCH
// 간선 목록을 주어진 순서대로 한 번만 완화한다. 목록이 위상 순서로 놓여 있지 않으면 거리가 덜 줄어든다.
fun dagShortestPaths(n: Int, edges: IntArray, source: Int): IntArray {
    val dist = LongArray(n) { Long.MAX_VALUE }
    dist[source] = 0
    for (e in 0 until edges.size / 3) {
        val u = edges[3 * e]; val v = edges[3 * e + 1]
        if (dist[u] != Long.MAX_VALUE && dist[u] + edges[3 * e + 2] < dist[v]) dist[v] = dist[u] + edges[3 * e + 2]
    }
    return IntArray(n) { if (dist[it] == Long.MAX_VALUE) Int.MAX_VALUE else dist[it].toInt() }
}
