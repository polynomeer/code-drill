// kind: WRONG_BRANCH
// 꺼낸 목록의 다음 원소를 넣지 않는다. 목록마다 첫 원소만 나온다.
fun mergeSorted(sizes: IntArray, values: IntArray): IntArray {
    val heap = java.util.PriorityQueue<Int>()
    var at = 0
    for (n in sizes) { if (n > 0) heap.add(values[at]); at += n }
    val out = ArrayList<Int>()
    while (heap.isNotEmpty()) out.add(heap.poll())
    return out.toIntArray()
}
