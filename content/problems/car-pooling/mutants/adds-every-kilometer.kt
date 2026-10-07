// kind: PERFORMANCE
// 여정마다 지나는 km 를 하나씩 더한다. 여정이 길면 여정 수 × 거리만큼 돈다.
fun carPooling(trips: IntArray, capacity: Int): Int {
    val load = IntArray(1_000_001)
    for (i in trips.indices step 3) {
        for (km in trips[i + 1] until trips[i + 2]) {
            load[km] += trips[i]
            if (load[km] > capacity) return 0
        }
    }
    return 1
}
