// 검증용 정답 (§6.1 solutions/). 목록마다 앞 원소 하나씩만 힙에 둔다. O(N log k).
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    val start = IntArray(sizes.size + 1)
    for (i in sizes.indices) start[i + 1] = start[i] + sizes[i]
    val out = IntArray(values.size)
    // (값, 목록 번호, values 안의 자리)
    val heap = java.util.PriorityQueue<IntArray>(compareBy { it[0] })
    for (i in sizes.indices) if (sizes[i] > 0) { heap.add(intArrayOf(values[start[i]], i, start[i])); Drill.push(values[start[i]]) }
    var n = 0
    while (heap.isNotEmpty()) {
        val (v, i, j) = heap.poll()
        Drill.pop(v)
        out[n++] = v
        if (j + 1 < start[i + 1]) { heap.add(intArrayOf(values[j + 1], i, j + 1)); Drill.push(values[j + 1]) }
    }
    return out
}
