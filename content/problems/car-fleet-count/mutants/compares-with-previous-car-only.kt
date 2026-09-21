// kind: WRONG_ALGORITHM
// 바로 앞 차의 시간과만 비교한다. 무리의 시간(가장 느린 것)이 아니라 앞차 자신의 시간이라 사슬 흡수를 놓친다.
fun carFleetCount(target: Int, positions: IntArray, speeds: IntArray): Int {
    val order = positions.indices.sortedByDescending { positions[it] }
    var fleets = 0
    var previous = -1.0
    for (i in order) {
        val time = (target - positions[i]).toDouble() / speeds[i]
        if (time > previous) fleets += 1
        previous = time
    }
    return fleets
}
