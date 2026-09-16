// kind: PERFORMANCE
// 누적합의 모든 쌍을 본다. O(n²).
fun shortestSubarrayAtLeastK(nums: IntArray, k: Int): Int {
    val n = nums.size
    val prefix = LongArray(n + 1)
    for (i in 0 until n) prefix[i + 1] = prefix[i] + nums[i]
    var best = n + 1
    for (i in 0..n) for (j in i + 1..n) { Drill.compare(i, j); if (prefix[j] - prefix[i] >= k && j - i < best) best = j - i }
    return if (best <= n) best else -1
}
