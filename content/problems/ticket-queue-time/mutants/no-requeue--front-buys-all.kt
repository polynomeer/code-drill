// kind: WRONG_ALGORITHM
// 맨 앞사람이 표를 다 살 때까지 줄이 움직이지 않는다. 뒤로 가서 다시 서지 않는다.
fun ticketQueueTime(tickets: IntArray, k: Int): Int {
    var seconds = 0
    for (i in 0..k) seconds += tickets[i]
    return seconds
}
