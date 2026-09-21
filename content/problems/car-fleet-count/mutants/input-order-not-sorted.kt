// kind: MISSING_EDGE_CASE
// 위치로 정렬하지 않고 입력 순서로 본다.
fun carFleetCount(target: Int, positions: IntArray, speeds: IntArray): Int {
    var fleets = 0
    var slowest = -1.0
    for (i in positions.indices) {
        val time = (target - positions[i]).toDouble() / speeds[i]
        if (time > slowest) { fleets += 1; slowest = time }
    }
    return fleets
}
