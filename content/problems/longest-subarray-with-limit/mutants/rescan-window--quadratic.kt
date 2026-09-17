// kind: PERFORMANCE
// 창을 늘릴 때마다 최댓값·최솟값을 다시 훑는다. O(n²).
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    var best = 0
    for (i in nums.indices) {
        var lo = nums[i]; var hi = nums[i]
        for (j in i until nums.size) {
            Drill.compare(i, j)
            if (nums[j] < lo) lo = nums[j]
            if (nums[j] > hi) hi = nums[j]
            if (hi.toLong() - lo > limit) break
            if (j - i + 1 > best) best = j - i + 1
        }
    }
    return best
}
