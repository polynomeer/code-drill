// kind: PERFORMANCE
// 회의마다 자기 시작 시각에 열려 있는 회의를 전부 센다. O(n²).
fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    var best = 0
    for (i in 0 until n) {
        val start = intervals[2 * i]
        var open = 0
        for (j in 0 until n) {
            Drill.compare(i, j)
            if (intervals[2 * j] <= start && start < intervals[2 * j + 1]) open += 1
        }
        if (open > best) best = open
    }
    return best
}
