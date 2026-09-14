// kind: WRONG_BRANCH
// 마지막 밧줄의 길이를 답한다. 비용은 잇는 횟수마다 쌓인다.
fun connectCost(lengths: IntArray): Int {
    val heap = java.util.PriorityQueue<Int>()
    for (v in lengths) heap.add(v)
    while (heap.size > 1) heap.add(heap.poll() + heap.poll())
    return if (lengths.size == 1) 0 else heap.poll()
}
