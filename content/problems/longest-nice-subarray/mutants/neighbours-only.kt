// kind: WRONG_ALGORITHM
// 이웃한 두 수만 겹치는지 본다. 구간 안의 모든 쌍이 겹치지 않아야 한다.
fun longestNiceSubarray(nums: IntArray): Int {
    var best = 1
    var run = 1
    for (i in 1 until nums.size) {
        run = if (nums[i] and nums[i - 1] == 0) run + 1 else 1
        best = maxOf(best, run)
    }
    return best
}
