// 검증용 정답 (§6.1 solutions/). 최댓값·최솟값 단조 덱과 줄어들지 않는 창.
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>()
    val minDq = ArrayDeque<Int>()
    var left = 0
    var best = 0
    for (right in nums.indices) {
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast()
        maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast()
        minDq.addLast(right)
        while (nums[maxDq.first()].toLong() - nums[minDq.first()] > limit) {
            left += 1
            if (maxDq.first() < left) maxDq.removeFirst()
            if (minDq.first() < left) minDq.removeFirst()
        }
        Drill.compare(left, right)
        if (right - left + 1 > best) { best = right - left + 1; Drill.write(0, best) }
    }
    return best
}
