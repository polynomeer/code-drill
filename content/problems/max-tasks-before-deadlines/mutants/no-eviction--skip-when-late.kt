// kind: WRONG_ALGORITHM
// 마감을 넘기는 작업을 건너뛰기만 한다. 이미 넣은 긴 작업을 빼고 지금 것을 넣는 편이 나을 때를 놓친다.
fun maxTasksBeforeDeadlines(durations: IntArray, deadlines: IntArray): Int {
    val order = durations.indices.sortedBy { deadlines[it] }
    var elapsed = 0L
    var count = 0
    for (i in order) {
        if (elapsed + durations[i] <= deadlines[i]) { elapsed += durations[i]; count += 1 }
    }
    return count
}
