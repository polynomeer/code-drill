// kind: WRONG_BRANCH
// 합이 목표 이상이면 왼쪽을 한 칸만 줄인다. 더 줄일 수 있어도 멈춘다.
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        if (total >= target) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            total -= nums[left]; left += 1
        }
    }
    return best
}
