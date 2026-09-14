// kind: WRONG_ALGORITHM
// 가장 길게 연속으로 이어진 값을 답한다. 과반수는 흩어져 있어도 과반수다.
fun majority(nums: IntArray): Int {
    var best = nums[0]; var bestRun = 0
    var run = 0
    for (i in nums.indices) {
        run = if (i > 0 && nums[i] == nums[i - 1]) run + 1 else 1
        if (run > bestRun) { bestRun = run; best = nums[i] }
    }
    return best
}
