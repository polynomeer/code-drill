// kind: OFF_BY_ONE
// 곱이 k 이하인 구간을 센다. 미만이어야 한다.
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k < 1) return 0
    var product = 1L; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product > k) { product /= nums[left]; left += 1 }
        total += right - left + 1
    }
    return total.toInt()
}
