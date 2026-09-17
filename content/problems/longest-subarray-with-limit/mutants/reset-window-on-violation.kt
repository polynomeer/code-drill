// kind: WRONG_ALGORITHM
// 한도를 넘으면 창을 지금 자리에서 새로 시작한다. 한 칸만 줄이면 되는 창을 잃는다.
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>(); val minDq = ArrayDeque<Int>()
    var left = 0; var best = 0
    for (right in nums.indices) {
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast(); maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast(); minDq.addLast(right)
        if (nums[maxDq.first()].toLong() - nums[minDq.first()] > limit) { left = right; maxDq.clear(); minDq.clear(); maxDq.addLast(right); minDq.addLast(right) }
        if (right - left + 1 > best) best = right - left + 1
    }
    return best
}
