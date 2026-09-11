// kind: OFF_BY_ONE
// 끝나는 순간 시작하는 회의를 겹친 것으로 센다. 부등호 하나 차이다.
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    val ends = PriorityQueue<Int>()
    var best = 0
    for (i in order) {
        while (ends.isNotEmpty() && ends.peek() < intervals[2 * i]) ends.poll()
        ends.add(intervals[2 * i + 1])
        best = maxOf(best, ends.size)
    }
    return best
}
