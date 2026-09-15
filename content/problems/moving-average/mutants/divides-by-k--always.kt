// kind: WRONG_BRANCH
// 창이 아직 k개가 안 찼는데도 k 로 나눈다.
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v); total += v
        if (window.size > k) total -= window.removeFirst()
        out[i] = Math.floorDiv(total, k)
    }
    return out
}
