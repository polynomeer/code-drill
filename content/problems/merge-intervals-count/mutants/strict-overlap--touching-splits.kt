// kind: OFF_BY_ONE
// 맞닿은 구간을 떨어진 것으로 센다. 부등호 하나 차이다.
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    var count = 0
    var end = Long.MIN_VALUE
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        if (start >= end) { count += 1; end = finish.toLong() }
        else if (finish > end) end = finish.toLong()
    }
    return count
}
