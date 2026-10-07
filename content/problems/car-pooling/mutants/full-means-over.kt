// kind: WRONG_BRANCH
// 인원이 좌석 수와 같아도 넘쳤다고 본다. 꼭 차는 것은 태울 수 있다.
fun carPooling(trips: IntArray, capacity: Int): Int {
    val diff = IntArray(1_000_002)
    for (i in trips.indices step 3) { diff[trips[i + 1]] += trips[i]; diff[trips[i + 2]] -= trips[i] }
    var load = 0
    for (d in diff) { load += d; if (load >= capacity) return 0 }
    return 1
}
