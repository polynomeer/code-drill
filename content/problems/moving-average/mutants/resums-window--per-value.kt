// kind: PERFORMANCE
// 값마다 최근 k개를 다시 더한다. O(n·k).
fun movingAverage(values: IntArray, k: Int): IntArray {
    val out = IntArray(values.size)
    for (i in values.indices) {
        val from = maxOf(0, i - k + 1)
        var total = 0
        for (j in from..i) { Drill.visit(j, values[j]); total += values[j] }
        out[i] = Math.floorDiv(total, i - from + 1)
    }
    return out
}
