// 검증용 정답 (§6.1 solutions/). 차분 배열에 오르고 내리는 인원을 적고 앞에서부터 누적한다.
fun carPooling(trips: IntArray, capacity: Int): Int {
    val diff = IntArray(1_000_002)
    for (i in trips.indices step 3) {
        diff[trips[i + 1]] += trips[i]
        diff[trips[i + 2]] -= trips[i]
    }
    var load = 0
    for (km in diff.indices) {
        load += diff[km]
        if (diff[km] != 0) Drill.write(km, load)
        if (load > capacity) return 0
    }
    return 1
}
