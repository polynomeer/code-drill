// 검증용 정답 (§6.1 solutions/). 줄을 그대로 흉내 낸다.
fun ticketQueueTime(tickets: IntArray, k: Int): Int {
    val remaining = tickets.copyOf()
    val queue = ArrayDeque<Int>()
    for (i in tickets.indices) queue.addLast(i)
    var seconds = 0
    while (queue.isNotEmpty()) {
        val i = queue.removeFirst()
        Drill.dequeue(i)
        remaining[i] -= 1
        seconds += 1
        if (i == k && remaining[i] == 0) return seconds
        if (remaining[i] > 0) { queue.addLast(i); Drill.enqueue(i) }
    }
    return seconds
}
