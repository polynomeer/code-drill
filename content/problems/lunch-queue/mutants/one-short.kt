// kind: OFF_BY_ONE
// 막힌 샌드위치를 빼고 센다. 막힌 것부터 아래가 모두 남는다.
fun lunchQueue(students: IntArray, sandwiches: IntArray): Int {
    val want = IntArray(2)
    for (s in students) want[s] += 1
    for (k in sandwiches.indices) { val s = sandwiches[k]; if (want[s] == 0) return sandwiches.size - k - 1; want[s] -= 1 }
    return 0
}
