// kind: WRONG_BRANCH
// 왼쪽을 줄일 때 종류 수를 줄이지 않는다. 창이 다시 늘지 못한다.
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    fun atMost(limit: Int): Long {
        val counts = IntArray(nums.size + 1)
        var kinds = 0; var left = 0; var total = 0L
        for (right in nums.indices) {
            if (counts[nums[right]]++ == 0) kinds += 1
            while (kinds > limit && left <= right) { counts[nums[left]] -= 1; left += 1 }
            total += right - left + 1
        }
        return total
    }
    return (atMost(k) - atMost(k - 1)).toInt()
}
