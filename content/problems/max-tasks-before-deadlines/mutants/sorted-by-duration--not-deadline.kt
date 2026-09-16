// kind: WRONG_BRANCH
// 마감이 아니라 걸리는 시간 순으로 본다. 마감이 이른 것을 뒤로 미룬다.
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { durations[it] }
    val heap = java.util.PriorityQueue<Int>(compareByDescending { it })
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]; heap.add(durations[i])
        if (elapsed > deadlines[i]) elapsed -= heap.poll()
    }
    return heap.size
}
