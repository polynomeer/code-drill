// kind: WRONG_BRANCH
// 가장 오른쪽 균형점을 돌려준다.
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = 0L
    var found = -1
    for (i in nums.indices) {
        if (left == total - left - nums[i]) found = i
        left += nums[i]
    }
    return found
}
