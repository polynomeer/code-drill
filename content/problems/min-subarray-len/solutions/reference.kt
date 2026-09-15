// 검증용 정답 (§6.1 solutions/). 양쪽 끝이 각각 n 번만 움직이는 창.
fun minSubarrayLen(nums: IntArray, target: Int): Int {
    var best = 0
    var left = 0
    var total = 0L
    for (right in nums.indices) {
        total += nums[right]
        Drill.pointer("right", right)
        while (total >= target) {
            val length = right - left + 1
            if (best == 0 || length < best) { best = length; Drill.write(0, best) }
            total -= nums[left]
            left += 1
            Drill.pointer("left", left)
        }
    }
    return best
}
