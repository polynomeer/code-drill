// 검증용 정답 (§6.1 solutions/). 접두사 합이 처음 나온 자리를 기억하고, 다시 만나면 그 거리가 후보다.
fun longestZeroSum(nums: IntArray): Int {
    val first = HashMap<Long, Int>()
    first[0L] = -1
    var total = 0L
    var best = 0
    for (i in nums.indices) {
        total += nums[i]
        Drill.write(i, total.toInt())
        val seen = first[total]
        if (seen != null) {
            if (i - seen > best) { best = i - seen; Drill.match(seen, i) }
        } else {
            first[total] = i
        }
    }
    return best
}
