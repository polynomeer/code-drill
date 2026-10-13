// kind: OFF_BY_ONE
// 자기 자신을 왼쪽 합에 먼저 더한다. i 자신은 어느 쪽에도 들지 않는다.
fun leftRightDifference(nums: IntArray): IntArray {
    val total = nums.sum()
    var left = 0
    return IntArray(nums.size) { i -> left += nums[i]; Math.abs(left - (total - left)) }
}
