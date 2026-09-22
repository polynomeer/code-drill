// kind: WRONG_ALGORITHM
// 전체 합이 0 이면 -1 이라고 지름길을 둔다. 음수가 있으면 틀린다.
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    if (total == 0L && nums.size > 1) return -1
    var left = 0L
    for (i in nums.indices) {
        if (left == total - left - nums[i]) return i
        left += nums[i]
    }
    return -1
}
