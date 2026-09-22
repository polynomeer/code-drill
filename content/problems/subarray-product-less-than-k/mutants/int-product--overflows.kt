// kind: WRONG_BRANCH
// 곱을 Int 로 둔다. 1000⁴ 에서 넘친다.
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k <= 1) return 0
    var product = 1; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product >= k) { product /= nums[left]; left += 1 }
        total += right - left + 1
    }
    return total.toInt()
}
