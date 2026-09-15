// kind: MISSING_EDGE_CASE
// 정수 나눗셈을 그대로 쓴다. 음수 평균이 0 쪽으로 잘린다.
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v); total += v
        if (window.size > k) total -= window.removeFirst()
        out[i] = total / window.size
    }
    return out
}
