// 검증용 정답 (§6.1 solutions/). 이하 k − 이하 k−1.
fun subarraysWithKDistinct(nums: IntArray, k: Int): Int {
    fun atMost(limit: Int): Long {
        val counts = IntArray(nums.size + 1)
        var kinds = 0; var left = 0; var total = 0L
        for (right in nums.indices) {
            if (counts[nums[right]]++ == 0) kinds += 1
            while (kinds > limit) { if (--counts[nums[left]] == 0) kinds -= 1; left += 1 }
            Drill.compare(left, right)
            total += right - left + 1
        }
        return total
    }
    val answer = atMost(k) - atMost(k - 1)
    Drill.write(0, answer.toInt())
    return answer.toInt()
}
