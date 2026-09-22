// kind: OFF_BY_ONE
// 창의 길이를 right − left 로 센다. 원소 하나짜리 구간이 빠진다.
fun subarrayProductLessThanK(nums: IntArray, k: Int): Int {
    if (k <= 1) return 0
    var product = 1L; var left = 0; var total = 0L
    for (right in nums.indices) {
        product *= nums[right]
        while (product >= k) { product /= nums[left]; left += 1 }
        total += right - left
    }
    return total.toInt()
}
