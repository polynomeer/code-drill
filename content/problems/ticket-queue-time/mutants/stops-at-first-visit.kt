// kind: WRONG_BRANCH
// k 번째 사람이 처음 표를 사는 순간을 답한다. 마지막 표를 사는 순간이어야 한다.
fun ticketQueueTime(tickets: IntArray, k: Int): Int {
    val remaining = tickets.copyOf()
    val queue = ArrayDeque<Int>()
    for (i in tickets.indices) queue.addLast(i)
    var seconds = 0
    while (queue.isNotEmpty()) {
        val i = queue.removeFirst()
        remaining[i] -= 1
        seconds += 1
        if (i == k) return seconds
        if (remaining[i] > 0) queue.addLast(i)
    }
    return seconds
}
