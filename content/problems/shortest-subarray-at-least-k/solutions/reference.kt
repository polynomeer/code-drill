// 검증용 정답 (§6.1 solutions/). 누적합 위의 단조 증가 양쪽 큐.
fun shortestSubarrayAtLeastK(nums: IntArray, k: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val queue = IntArray(n + 1)
    var head = 0
    var tail = 0
    var best = n + 1
    for (j in 0..n) {
        while (head < tail && prefix[j] - prefix[queue[head]] >= k) {
            val i = queue[head++]
            Drill.dequeue(i)
            if (j - i < best) best = j - i
        }
        while (head < tail && prefix[queue[tail - 1]] >= prefix[j]) { Drill.pop(queue[tail - 1]); tail -= 1 }
        queue[tail++] = j
        Drill.enqueue(j)
    }
    return if (best <= n) best else -1
}
