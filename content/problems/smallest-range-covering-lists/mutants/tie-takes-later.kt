// kind: OFF_BY_ONE
// 같은 길이면 나중 것으로 바꾼다. lo 가 작은 것을 골라야 한다.
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
        if (currentMax.toLong() - low <= bestLength) { bestLength = currentMax.toLong() - low; bestLo = low; bestHi = currentMax }
        if (pointer[i] + 1 == sizes[i]) break
        pointer[i] += 1
        currentMax = maxOf(currentMax, values[start[i] + pointer[i]])
        heap.add(i)
    }
    return intArrayOf(bestLo, bestHi)
}
