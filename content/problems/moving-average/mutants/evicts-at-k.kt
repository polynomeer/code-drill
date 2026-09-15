// kind: OFF_BY_ONE
// 창이 k 개 이상이면 내보낸다. 정확히 k 개일 때도 내보내 창이 k-1 개다.
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v); total += v
        if (window.size >= k) total -= window.removeFirst()
        out[i] = Math.floorDiv(total, window.size)
    }
    return out
}
