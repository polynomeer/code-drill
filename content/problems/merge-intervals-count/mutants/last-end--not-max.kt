// kind: WRONG_BRANCH
// 덩어리의 끝을 마지막 구간의 끝으로 둔다. 큰 구간이 작은 것을 품으면 끝이 뒤로 물러난다.
fun mergeCount(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    var count = 0
    var end = Long.MIN_VALUE
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        if (start > end) count += 1
        end = finish.toLong()
    }
    return count
}
