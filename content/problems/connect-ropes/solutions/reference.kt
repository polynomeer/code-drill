// 검증용 정답 (§6.1 solutions/). 최소 힙에서 가장 짧은 둘을 꺼내 잇고 다시 넣는다.
fun connectCost(lengths: IntArray): Int {
    val heap = java.util.PriorityQueue<Int>()
    for (v in lengths) heap.add(v)
    var total = 0
    while (heap.size > 1) {
        val a = heap.poll()
        val b = heap.poll()
        Drill.compare(a, b)
        total += a + b
        Drill.push(a + b)
        heap.add(a + b)
        Drill.write(0, total)
    }
    return total
}
