// kind: OFF_BY_ONE
// k 번만 반복한다. 경유 k 번은 항공편 k+1 편이다.
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    var dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k) {
        val next = dist.copyOf()
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < next[b]) next[b] = dist[a] + w
        }
        dist = next
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
