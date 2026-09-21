// kind: OFF_BY_ONE
// 시간이 같으면 다른 무리로 센다. 목적지에서 정확히 따라잡으면 같은 무리다.
fun carFleetCount(target: Int, positions: IntArray, speeds: IntArray): Int {
    val order = positions.indices.sortedByDescending { positions[it] }
    var fleets = 0
    var slowest = -1.0
    for (i in order) {
        val time = (target - positions[i]).toDouble() / speeds[i]
        if (time >= slowest) { fleets += 1; slowest = time }
    }
    return fleets
}
