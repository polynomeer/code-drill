// kind: WRONG_ALGORITHM
// 왼쪽 합만 낸다. 오른쪽 합과의 차다.
fun leftRightDifference(nums: IntArray): IntArray {
    var left = 0
    return IntArray(nums.size) { i -> val v = left; left += nums[i]; v }
}
