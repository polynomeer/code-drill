// kind: WRONG_ALGORITHM
// 한 반복 안에서 방금 갱신한 값을 이어 쓴다. 편 수가 샌다.
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    val dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k + 1) {
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < dist[b]) dist[b] = dist[a] + w
        }
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
