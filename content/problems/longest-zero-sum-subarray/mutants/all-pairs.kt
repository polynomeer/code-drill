// kind: PERFORMANCE
// 모든 시작점에서 합을 다시 더해 본다. O(n^2).
fun longestZeroSum(nums: IntArray): Int {
    var best = 0
    for (l in nums.indices) {
        var total = 0L
        for (r in l until nums.size) {
            total += nums[r]
            Drill.compare(l, r)
            if (total == 0L && r - l + 1 > best) best = r - l + 1
        }
    }
    return best
}
