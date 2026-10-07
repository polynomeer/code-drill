// kind: OFF_BY_ONE
// 내리는 km 에서도 그 사람이 타 있다고 센다. 내리는 자리에서 타는 사람과 겹치지 않는다.
fun carPooling(trips: IntArray, capacity: Int): Int {
    val diff = IntArray(1_000_003)
    for (i in trips.indices step 3) { diff[trips[i + 1]] += trips[i]; diff[trips[i + 2] + 1] -= trips[i] }
    var load = 0
    for (d in diff) { load += d; if (load > capacity) return 0 }
    return 1
}
