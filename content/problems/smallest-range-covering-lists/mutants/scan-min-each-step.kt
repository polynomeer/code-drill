// kind: PERFORMANCE
// 힙 대신 매 걸음 k 개 포인터를 훑어 최솟값을 찾는다. O(N·k).
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        var minI = 0; var low = Int.MAX_VALUE; var high = Int.MIN_VALUE
        for (i in 0 until k) {
            val v = values[start[i] + pointer[i]]
            Drill.compare(i, v)
            if (v < low) { low = v; minI = i }
            if (v > high) high = v
        }
        if (high.toLong() - low < bestLength) { bestLength = high.toLong() - low; bestLo = low; bestHi = high }
        if (pointer[minI] + 1 == sizes[minI]) break
        pointer[minI] += 1
    }
    return intArrayOf(bestLo, bestHi)
}
