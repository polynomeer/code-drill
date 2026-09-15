// 검증용 정답 (§6.1 solutions/). 반복 횟수를 k+1 로 제한한 벨만-포드. 반복마다 이전 표를 따로 둔다.
fun cheapestWithStops(n: Int, flights: IntArray, src: Int, dst: Int, k: Int): Int {
    val inf = Int.MAX_VALUE / 2
    var dist = IntArray(n) { inf }
    dist[src] = 0
    repeat(k + 1) {
        val next = dist.copyOf()
        for (i in flights.indices step 3) {
            val a = flights[i]; val b = flights[i + 1]; val w = flights[i + 2]
            if (dist[a] != inf && dist[a] + w < next[b]) {
                next[b] = dist[a] + w
                Drill.edge("c$a", "c$b")
                Drill.write(b, next[b])
            }
        }
        dist = next
    }
    return if (dist[dst] == inf) -1 else dist[dst]
}
