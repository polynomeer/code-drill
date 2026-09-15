// kind: WRONG_BRANCH
// 목적지에 처음 닿은 반복에서 멈춘다. 더 많은 편으로 더 싼 길이 있을 수 있다.
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    var dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k + 1) {
        val next = dist.copyOf()
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < next[b]) next[b] = dist[a] + w
        }
        dist = next
        if (dist[dst] != inf) return dist[dst]
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
