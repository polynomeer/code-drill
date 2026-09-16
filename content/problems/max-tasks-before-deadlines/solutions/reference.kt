// 검증용 정답 (§6.1 solutions/). 마감순으로 넣고 넘치면 가장 긴 것을 뺀다.
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    val heap = java.util.PriorityQueue<Int>(compareByDescending { it })
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]
        heap.add(durations[i])
        Drill.push(durations[i])
        Drill.compare(elapsed.toInt(), deadlines[i])
        if (elapsed > deadlines[i]) {
            val longest = heap.poll()
            Drill.pop(longest)
            elapsed -= longest
        }
    }
    return heap.size
}
