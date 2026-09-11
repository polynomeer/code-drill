// kind: MISSING_EDGE_CASE
// 시작 순으로 정렬하지 않는다. 입력이 시간 순이면 맞고 아니면 방을 돌려받을 타이밍이 어긋난다.
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val ends = PriorityQueue<Int>()
    var best = 0
    for (i in 0 until intervals.size / 2) {
        while (ends.isNotEmpty() && ends.peek() <= intervals[2 * i]) ends.poll()
        ends.add(intervals[2 * i + 1])
        best = maxOf(best, ends.size)
    }
    return best
}
