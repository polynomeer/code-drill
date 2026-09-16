// kind: OFF_BY_ONE
// 누적합의 0 번 자리를 큐에 넣지 않는다. 배열의 처음에서 시작하는 구간을 못 센다.
fun shortestSubarrayAtLeastK(nums: IntArray, k: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    val queue = ArrayDeque<Int>()
    var best = n + 1
    for (j in 1..n) {
        while (queue.isNotEmpty() && prefix[j] - prefix[queue.first()] >= k) best = minOf(best, j - queue.removeFirst())
        while (queue.isNotEmpty() && prefix[queue.last()] >= prefix[j]) queue.removeLast()
        queue.addLast(j)
    }
    return if (best <= n) best else -1
}
