// 검증용 정답 (§6.1 solutions/). 곱을 Long 으로 유지하는 창.
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k <= 1) return 0
    var product = 1L; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product >= k) { product /= nums[left]; left += 1 }
        Drill.compare(left, right)
        total += right - left + 1
        Drill.write(0, total.toInt())
    }
    return total.toInt()
}
