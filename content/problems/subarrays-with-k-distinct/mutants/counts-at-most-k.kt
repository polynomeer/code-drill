// kind: WRONG_ALGORITHM
// k 개 이하인 구간을 센다. 정확히 k 가 아니다.
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    val counts = IntArray(nums.size + 1)
    var kinds = 0; var left = 0; var total = 0L
    for (right in nums.indices) {
        if (counts[nums[right]]++ == 0) kinds += 1
        while (kinds > k) { if (--counts[nums[left]] == 0) kinds -= 1; left += 1 }
        total += right - left + 1
    }
    return total.toInt()
}
