// kind: WRONG_ALGORITHM
// 창을 줄여도 덱의 앞을 빼지 않는다. 나간 원소가 최댓값·최솟값으로 남는다.
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>(); val minDq = ArrayDeque<Int>()
    var left = 0; var best = 0
    for (right in nums.indices) {
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast(); maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast(); minDq.addLast(right)
        while (nums[maxDq.first()].toLong() - nums[minDq.first()] > limit && left < right) left += 1
        if (right - left + 1 > best) best = right - left + 1
    }
    return best
}
