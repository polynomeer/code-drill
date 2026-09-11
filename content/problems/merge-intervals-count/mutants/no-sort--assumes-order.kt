// kind: MISSING_EDGE_CASE
// 정렬하지 않는다. 입력이 시작점 순이면 맞고 아니면 틀린다.
fun mergeCount(intervals: IntArray): Int {
    var count = 0
    var end = Long.MIN_VALUE
    for (i in 0 until intervals.size / 2) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        if (start > end) { count += 1; end = finish.toLong() }
        else if (finish > end) end = finish.toLong()
    }
    return count
}
