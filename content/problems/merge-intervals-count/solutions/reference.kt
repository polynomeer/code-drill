// 검증용 정답 (§6.1 solutions/). 시작점 정렬 + 한 번 훑기.
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    var count = 0
    var end = Long.MIN_VALUE
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        Drill.visit(i, start)
        Drill.compare(i, if (end == Long.MIN_VALUE) -1 else end.toInt())
        if (start > end) {
            count += 1
            Drill.match(i, count)
            end = finish.toLong()
        } else if (finish > end) {
            end = finish.toLong()
        }
    }
    return count
}
