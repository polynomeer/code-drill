// 검증용 정답 (§6.1 solutions/). 창의 합을 유지하고 큐가 나갈 값을 기억한다.
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    val window = ArrayDeque<Int>()
    var total = 0
    for ((i, v) in values.withIndex()) {
        window.addLast(v)
        Drill.enqueue(v)
        total += v
        if (window.size > k) {
            val gone = window.removeFirst()
            Drill.dequeue(gone)
            total -= gone
        }
        out[i] = Math.floorDiv(total, window.size)
        Drill.write(i, out[i])
    }
    return out
}
