// kind: WRONG_ALGORITHM
// 구간의 첫 풍선을 먼저 터뜨리는 것으로 나눈다 — 이웃이 원래 자리의 것이라 부분 문제가 독립이 아니다.
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    val n = vals.size
    val best = Array(n) { IntArray(n) }
    for (length in 2 until n) for (left in 0 until n - length) {
        val right = left + length
        var top = 0
        for (k in left + 1 until right) top = maxOf(top, best[left][k] + best[k][right] + vals[k - 1] * vals[k] * vals[k + 1])
        best[left][right] = top
    }
    return best[0][n - 1]
}
