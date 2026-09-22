// 검증용 정답 (§6.1 solutions/). 전체 합에서 왼쪽 합을 빼며 한 번.
fun pivotIndex(nums: IntArray): Int {
    var total = 0L
    for (x in nums) total += x
    var left = 0L
    for (i in nums.indices) {
        Drill.compare(i, left.toInt())
        if (left == total - left - nums[i]) { Drill.write(0, i); return i }
        left += nums[i]
    }
    return -1
}
