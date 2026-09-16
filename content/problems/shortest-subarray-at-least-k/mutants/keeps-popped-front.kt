// kind: WRONG_BRANCH
// 답 후보를 만든 앞자리를 큐에 남긴다. 결과는 맞지만 큐가 단조롭지 않게 되어 뒤에서 잘못 뺀다.
fun shortestSubarrayAtLeastK(nums: IntArray, k: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val queue = ArrayDeque<Int>()
    var best = n + 1
    for (j in 0..n) {
        for (i in queue) if (prefix[j] - prefix[i] >= k) best = minOf(best, j - i)
        while (queue.isNotEmpty() && prefix[queue.last()] > prefix[j]) queue.removeLast()
        queue.addLast(j)
        if (queue.size > 2) queue.removeFirst()
    }
    return if (best <= n) best else -1
}
