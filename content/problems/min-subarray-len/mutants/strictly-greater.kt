// kind: OFF_BY_ONE
// 합이 목표보다 커야 한다고 본다. 정확히 같은 합을 놓친다.
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        while (total > target) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            total -= nums[left]; left += 1
        }
    }
    return best
}
