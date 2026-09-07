// kind: MISSING_EDGE_CASE
// 연속한 구간만 센다. 떨어져 있는 원소를 이어 붙이지 못한다.
fun longestIncreasing(nums: IntArray): Int {
    var best = 1
    var run = 1
    for (i in 1 until nums.size) {
        run = if (nums[i] > nums[i - 1]) run + 1 else 1
        if (run > best) best = run
    }
    return best
}
