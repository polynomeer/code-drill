// kind: OFF_BY_ONE
// k 뒤에 선 사람도 k 가 끝나기 전에 min(t, tickets[k]) 장을 산 것으로 센다. 한 장 덜 사야 한다.
fun ticketQueueTime(tickets: IntArray, k: Int): Int {
    var seconds = 0
    for (i in tickets.indices) seconds += minOf(tickets[i], tickets[k])
    return seconds
}
