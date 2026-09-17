// kind: MISSING_EDGE_CASE
// 지울 것을 개수가 아니라 집합으로 둔다. 같은 값이 두 번 나가면 하나만 지워진다.
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashSet<Int>()
    var lowSize = 0; var highSize = 0
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty() && delayed.remove(heap.peek())) heap.poll()
    }
    fun balance() {
        if (lowSize > highSize + 1) { high.add(low.poll()); lowSize -= 1; highSize += 1; prune(low) }
        else if (lowSize < highSize) { low.add(high.poll()); highSize -= 1; lowSize += 1; prune(high) }
    }
    fun add(x: Int) { if (low.isEmpty() || x <= low.peek()) { low.add(x); lowSize += 1 } else { high.add(x); highSize += 1 }; balance() }
    fun remove(x: Int) {
        delayed.add(x)
        if (low.isNotEmpty() && x <= low.peek()) { lowSize -= 1; if (x == low.peek()) prune(low) } else { highSize -= 1; if (high.isNotEmpty() && x == high.peek()) prune(high) }
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) { add(nums[i]); if (i >= k) remove(nums[i - k]); if (i >= k - 1) out[i - k + 1] = low.peek() }
    return out
}
