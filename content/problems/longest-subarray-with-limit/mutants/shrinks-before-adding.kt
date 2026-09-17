// kind: OFF_BY_ONE
// 새 원소를 넣기 전에 한도를 검사한다. 새 원소가 한도를 깨도 그 창을 센다.
fun longestSubarrayWithLimit(nums: IntArray, limit: Int): Int {
    val maxDq = ArrayDeque<Int>(); val minDq = ArrayDeque<Int>()
    var left = 0; var best = 0
    for (right in nums.indices) {
        while (maxDq.isNotEmpty() && minDq.isNotEmpty() && nums[maxDq.first()].toLong() - nums[minDq.first()] > limit) { left += 1; if (maxDq.first() < left) maxDq.removeFirst(); if (minDq.first() < left) minDq.removeFirst() }
        val x = nums[right]
        while (maxDq.isNotEmpty() && nums[maxDq.last()] < x) maxDq.removeLast(); maxDq.addLast(right)
        while (minDq.isNotEmpty() && nums[minDq.last()] > x) minDq.removeLast(); minDq.addLast(right)
        if (right - left + 1 > best) best = right - left + 1
    }
    return best
}
