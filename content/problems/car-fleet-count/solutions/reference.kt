// 검증용 정답 (§6.1 solutions/). 위치 내림차순으로 시간을 보며 무리를 센다.
fun carFleetCount(target: Int, positions: IntArray, speeds: IntArray): Int {
    val order = positions.indices.sortedByDescending { positions[it] }
    var fleets = 0
    var slowest = -1.0
    for (i in order) {
        val time = (target - positions[i]).toDouble() / speeds[i]
        Drill.compare(i, fleets)
        if (time > slowest) { fleets += 1; slowest = time; Drill.write(0, fleets) }
    }
    return fleets
}
