// kind: MISSING_EDGE_CASE
// 빈 부분 배열을 허용해 전부 음수인 입력에서 0 을 반환한다.
fun maxSubarray(nums: IntArray): Int {
    var current = 0
    var best = 0
    for (value in nums) {
        current = maxOf(0, current + value)
        best = maxOf(best, current)
    }
    return best
}
