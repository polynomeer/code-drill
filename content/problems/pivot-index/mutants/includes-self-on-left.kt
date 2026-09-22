// kind: OFF_BY_ONE
// 자기 자신을 왼쪽 합에 넣고 비교한다.
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = 0L
    for (i in nums.indices) {
        left += nums[i]
        if (left == total - left) return i
    }
    return -1
}
