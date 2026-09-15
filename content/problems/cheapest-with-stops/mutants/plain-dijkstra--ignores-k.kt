// kind: WRONG_ALGORITHM
// 편 수 제한을 무시하고 가장 싼 길을 답한다.
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val adj = Array(n) { ArrayList<IntArray>() }
    for (i in flights.indices step 3) adj[flights[i]].add(intArrayOf(flights[i + 1], flights[i + 2]))
    val dist = IntArray(n) { inf }
    dist[src] = 0
    val heap = java.util.PriorityQueue<IntArray>(compareBy { it[1] })
    heap.add(intArrayOf(src, 0))
    while (heap.isNotEmpty()) {
        val (v, d) = heap.poll()
        if (d > dist[v]) continue
        for (e in adj[v]) if (d + e[1] < dist[e[0]]) { dist[e[0]] = d + e[1]; heap.add(intArrayOf(e[0], dist[e[0]])) }
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
