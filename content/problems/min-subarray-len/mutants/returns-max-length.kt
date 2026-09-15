// kind: WRONG_ALGORITHM
// 가장 긴 구간을 답한다.
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        while (total >= target) {
            best = maxOf(best, right - left + 1)
            total -= nums[left]; left += 1
        }
    }
    return best
}
