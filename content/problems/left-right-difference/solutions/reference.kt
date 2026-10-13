// 검증용 정답 (§6.1 solutions/). 전체 합에서 왼쪽 합과 자기 자신을 빼면 오른쪽 합이다.
fun leftRightDifference(nums: IntArray): IntArray {
    var total = 0
    for (v in nums) total += v
    var left = 0
    val out = IntArray(nums.size)
    for (i in nums.indices) {
        val right = total - left - nums[i]
        out[i] = Math.abs(left - right)
        Drill.write(i, out[i])
        left += nums[i]
    }
    return out
}
