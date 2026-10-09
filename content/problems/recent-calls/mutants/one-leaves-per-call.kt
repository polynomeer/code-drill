// kind: WRONG_BRANCH
// 호출마다 많아야 하나만 뺀다. 긴 공백 뒤에는 여러 호출이 한꺼번에 창을 벗어난다.
fun recentCalls(times: IntArray): IntArray {
    val out = IntArray(times.size)
    var head = 0
    for (i in times.indices) {
        if (times[head] < times[i] - 3000) head += 1
        out[i] = i - head + 1
    }
    return out
}
