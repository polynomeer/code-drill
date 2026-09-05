// 검증용 정답 (§6.1 solutions/). Kadane 알고리즘, O(n).
fun maxSubarray(nums: IntArray): Int {
    var current = nums[0]
    var best = nums[0]
    for (i in 1 until nums.size) {
        Drill.visit(i, nums[i])
        current = maxOf(nums[i], current + nums[i])
        if (current > best) {
            best = current
            Drill.match(i, best)
        } else {
            Drill.compare(i, best)
        }
    }
    return best
}
