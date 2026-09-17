// 검증용 정답 (§6.1 solutions/). 최대 힙 + 최소 힙, 미룬 삭제, 살아 있는 수로 균형.
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashMap<Int, Int>()
    var lowSize = 0; var highSize = 0
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty()) {
            val top = heap.peek()
            val pending = delayed[top] ?: 0
            if (pending == 0) break
            if (pending == 1) delayed.remove(top) else delayed[top] = pending - 1
            heap.poll()
        }
    }
    fun balance() {
        if (lowSize > highSize + 1) { high.add(low.poll()); lowSize -= 1; highSize += 1; prune(low) }
        else if (lowSize < highSize) { low.add(high.poll()); highSize -= 1; lowSize += 1; prune(high) }
    }
    fun add(x: Int) {
        if (low.isEmpty() || x <= low.peek()) { low.add(x); lowSize += 1 } else { high.add(x); highSize += 1 }
        balance()
    }
    fun remove(x: Int) {
        delayed[x] = (delayed[x] ?: 0) + 1
        if (low.isNotEmpty() && x <= low.peek()) { lowSize -= 1; if (x == low.peek()) prune(low) }
        else { highSize -= 1; if (high.isNotEmpty() && x == high.peek()) prune(high) }
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) {
        add(nums[i])
        if (i >= k) remove(nums[i - k])
        if (i >= k - 1) { out[i - k + 1] = low.peek(); Drill.compare(i, out[i - k + 1]); Drill.write(i - k + 1, out[i - k + 1]) }
    }
    return out
}
