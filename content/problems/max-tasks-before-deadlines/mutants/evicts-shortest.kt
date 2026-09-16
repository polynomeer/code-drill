// kind: WRONG_BRANCH
// 넘치면 가장 짧은 작업을 뺀다. 시간을 가장 적게 되찾는다.
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    val heap = java.util.PriorityQueue<Int>()
    var elapsed = 0L
    for (i in order) {
        elapsed += durations[i]; heap.add(durations[i])
        if (elapsed > deadlines[i]) elapsed -= heap.poll()
    }
    return heap.size
}
