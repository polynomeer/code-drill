// kind: OFF_BY_ONE
// 창마다 하나만 센다. 창 안의 모든 끝이 답이다.
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    fun atMost(limit: Int): Long {
        val counts = IntArray(nums.size + 1)
        var kinds = 0; var left = 0; var total = 0L
        for (right in nums.indices) {
            if (counts[nums[right]]++ == 0) kinds += 1
            while (kinds > limit) { if (--counts[nums[left]] == 0) kinds -= 1; left += 1 }
            if (right >= left) total += 1
        }
        return total
    }
    return (atMost(k) - atMost(k - 1)).toInt()
}
