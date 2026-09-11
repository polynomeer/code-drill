// kind: WRONG_ALGORITHM
// 가장 늦게 끝나는 회의를 먼저 본다. 방을 돌려받을 기회를 놓친다.
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    val ends = PriorityQueue<Int>(compareByDescending { it })
    var best = 0
    for (i in order) {
        while (ends.isNotEmpty() && ends.peek() <= intervals[2 * i]) ends.poll()
        ends.add(intervals[2 * i + 1])
        best = maxOf(best, ends.size)
    }
    return best
}
