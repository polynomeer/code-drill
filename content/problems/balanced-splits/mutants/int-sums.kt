// kind: MISSING_EDGE_CASE
// 합을 Int 로 더한다. 원소 하나가 10^9 라 셋만 더해도 넘친다.
fun balancedSplits(nums: IntArray): Int {
    var total = 0
    for (v in nums) total += v
    var left = 0; var count = 0
    for (i in 0 until nums.size - 1) { left += nums[i]; if (2 * left == total) count += 1 }
    return count
}
