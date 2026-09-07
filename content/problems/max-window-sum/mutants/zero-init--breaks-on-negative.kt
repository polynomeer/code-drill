// kind: MISSING_EDGE_CASE
// 최댓값을 0 으로 초기화해 전부 음수인 입력에서 0 을 돌려준다.
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in 0 until k) total += nums[i]
    var best = 0
    for (i in k until nums.size) {
        total += nums[i] - nums[i - k]
        if (total > best) best = total
    }
    return best
}
