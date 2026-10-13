// kind: MISSING_EDGE_CASE
// 절댓값을 취하지 않는다. 왼쪽이 더 무거우면 차가 음수가 된다.
fun leftRightDifference(nums: IntArray): IntArray {
    val total = nums.sum()
    var left = 0
    return IntArray(nums.size) { i -> val right = total - left - nums[i]; val d = right - left; left += nums[i]; d }
}
