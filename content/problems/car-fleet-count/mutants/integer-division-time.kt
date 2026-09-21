// kind: WRONG_BRANCH
// 시간을 정수 나눗셈으로 잰다. 소수점이 갈라야 할 무리가 합쳐진다.
fun carFleetCount(target: Int, positions: IntArray, speeds: IntArray): Int {
    val order = positions.indices.sortedByDescending { positions[it] }
    var fleets = 0
    var slowest = -1L
    for (i in order) {
        val time = ((target - positions[i]) / speeds[i]).toLong()
        if (time > slowest) { fleets += 1; slowest = time }
    }
    return fleets
}
