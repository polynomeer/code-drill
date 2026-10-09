// kind: OFF_BY_ONE
// 정확히 3000 앞의 호출까지 뺀다. 창은 양 끝을 포함한다.
fun recentCalls(times: IntArray): IntArray {
    val out = IntArray(times.size)
    var head = 0
    for (i in times.indices) {
        while (times[head] <= times[i] - 3000) head += 1
        out[i] = i - head + 1
    }
    return out
}
