// kind: WRONG_ALGORITHM
// 접두사 합의 자리를 볼 때마다 덮어쓴다. 가장 먼 짝 대신 가까운 짝을 잰다.
fun longestZeroSum(nums: IntArray): Int {
    val seen = HashMap<Long, Int>()
    seen[0L] = -1
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        val at = seen[total]
        if (at != null && i - at > best) best = i - at
        seen[total] = i
    }
    return best
}
