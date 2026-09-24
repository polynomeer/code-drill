// kind: MISSING_EDGE_CASE
// 빈 접두사를 넣지 않는다. 맨 앞에서 시작하는 구간을 놓친다.
fun longestZeroSum(nums: IntArray): Int {
    val first = HashMap<Long, Int>()
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        val at = first[total]
        if (at != null) { if (i - at > best) best = i - at } else first[total] = i
    }
    return best
}
