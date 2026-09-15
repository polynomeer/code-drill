// kind: OFF_BY_ONE
// 목록의 끝을 보지 않고 다음 자리를 넣는다. 다음 목록의 첫 원소가 이 목록 것처럼 들어와 순서가 깨진다.
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    val out = IntArray(values.size)
    val heap = java.util.PriorityQueue<Int>(compareBy { values[it] })
    var at = 0
    for (n in sizes) { if (n > 0) heap.add(at); at += n }
    var n = 0
    while (heap.isNotEmpty()) {
        val j = heap.poll()
        out[n++] = values[j]
        if (j + 1 < values.size) heap.add(j + 1)
    }
    return out
}
