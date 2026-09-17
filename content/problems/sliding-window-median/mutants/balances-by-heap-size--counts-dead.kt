// kind: WRONG_BRANCH
// 균형을 살아 있는 수가 아니라 힙의 크기로 잡는다. 지울 것으로 적어 둔 원소가 절반을 부풀린다.
fun slidingWindowMedian(nums: IntArray, k: Int): IntArray {
    val low = java.util.PriorityQueue<Int>(compareByDescending { it })
    val high = java.util.PriorityQueue<Int>()
    val delayed = HashMap<Int, Int>()
    fun prune(heap: java.util.PriorityQueue<Int>) {
        while (heap.isNotEmpty()) { val top = heap.peek(); val pending = delayed[top] ?: 0; if (pending == 0) break; if (pending == 1) delayed.remove(top) else delayed[top] = pending - 1; heap.poll() }
    }
    fun balance() {
        if (low.size > high.size + 1) { high.add(low.poll()); prune(low) }
        else if (low.size < high.size) { low.add(high.poll()); prune(high) }
    }
    fun add(x: Int) { if (low.isEmpty() || x <= low.peek()) low.add(x) else high.add(x); balance() }
    fun remove(x: Int) {
        delayed[x] = (delayed[x] ?: 0) + 1
        if (low.isNotEmpty() && x <= low.peek()) { if (x == low.peek()) prune(low) } else { if (high.isNotEmpty() && x == high.peek()) prune(high) }
        balance()
    }
    val out = IntArray(nums.size - k + 1)
    for (i in nums.indices) { add(nums[i]); if (i >= k) remove(nums[i - k]); if (i >= k - 1) out[i - k + 1] = low.peek() }
    return out
}
