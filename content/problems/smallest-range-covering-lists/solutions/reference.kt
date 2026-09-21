// 검증용 정답 (§6.1 solutions/). 최소 힙이 min 을, 누적 최댓값이 max 를 준다.
fun smallestRangeCoveringLists(values: IntArray, sizes: IntArray): IntArray {
    val k = sizes.size
    val start = IntArray(k)
    for (i in 1 until k) start[i] = start[i - 1] + sizes[i - 1]
    val pointer = IntArray(k)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[start[it] + pointer[it]] })
    var currentMax = Int.MIN_VALUE
    for (i in 0 until k) { heap.add(i); currentMax = maxOf(currentMax, values[start[i]]) }
    var bestLo = 0; var bestHi = 0; var bestLength = Long.MAX_VALUE
    while (true) {
        val i = heap.poll()
        val low = values[start[i] + pointer[i]]
        Drill.compare(low, currentMax)
        if (currentMax.toLong() - low < bestLength) { bestLength = currentMax.toLong() - low; bestLo = low; bestHi = currentMax; Drill.write(0, bestLength.toInt()) }
        if (pointer[i] + 1 == sizes[i]) break
        pointer[i] += 1
        currentMax = maxOf(currentMax, values[start[i] + pointer[i]])
        heap.add(i)
    }
    return intArrayOf(bestLo, bestHi)
}
