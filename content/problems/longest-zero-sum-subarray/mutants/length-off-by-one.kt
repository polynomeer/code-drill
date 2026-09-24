// kind: OFF_BY_ONE
// 구간의 길이를 하나 짧게 센다. 두 자리 사이의 칸 수가 곧 길이다.
fun longestZeroSum(nums: IntArray): Int {
    val first = HashMap<Long, Int>()
    first[0L] = -1
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        val at = first[total]
        if (at != null) { if (i - at - 1 > best) best = i - at - 1 } else first[total] = i
    }
    return best
}
