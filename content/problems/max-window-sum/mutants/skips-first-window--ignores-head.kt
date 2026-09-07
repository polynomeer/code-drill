// kind: OFF_BY_ONE
// 첫 창을 후보에 넣지 않아 최댓값이 맨 앞에 있으면 놓친다.
fun maxWindowSum(nums: IntArray, k: Int): Int {
    var total = 0
    for (i in 0 until k) total += nums[i]
    var best = Int.MIN_VALUE
    for (i in k until nums.size) {
        total += nums[i] - nums[i - k]
        if (total > best) best = total
    }
    return if (best == Int.MIN_VALUE) total else best
}
