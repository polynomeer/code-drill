// 검증용 정답 (§6.1 solutions/). 시작 순 정렬 + 끝나는 시각의 최소 힙.
import java.util.PriorityQueue

fun minRooms(intervals: IntArray): Int {
    val n = intervals.size / 2
    val order = (0 until n).sortedBy { intervals[2 * it] }
    val ends = PriorityQueue<Int>()
    var best = 0
    for (i in order) {
        val start = intervals[2 * i]
        val finish = intervals[2 * i + 1]
        while (ends.isNotEmpty() && ends.peek() <= start) {
            Drill.pop(ends.poll())
        }
        ends.add(finish)
        Drill.push(finish)
        if (ends.size > best) {
            best = ends.size
            Drill.match(i, best)
        }
    }
    return best
}
