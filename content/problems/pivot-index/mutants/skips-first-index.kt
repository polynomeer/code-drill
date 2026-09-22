// kind: MISSING_EDGE_CASE
// 맨 왼쪽 자리는 보지 않는다. 왼쪽 합 0 도 합이다.
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = nums[0].toLong()
    for (i in 1 until nums.size) {
        if (left == total - left - nums[i]) return i
        left += nums[i]
    }
    return -1
}
