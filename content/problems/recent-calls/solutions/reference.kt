// 검증용 정답 (§6.1 solutions/). 큐 뒤에 넣고, 앞에서 창 밖의 호출을 모두 뺀다.
fun recentCalls(times: IntArray): IntArray {
    val out = IntArray(times.size)
    var head = 0
    for (i in times.indices) {
        Drill.enqueue(times[i])
        while (times[head] < times[i] - 3000) { Drill.dequeue(times[head]); head += 1 }
        out[i] = i - head + 1
    }
    return out
}
