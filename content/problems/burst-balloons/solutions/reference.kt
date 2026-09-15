// 검증용 정답 (§6.1 solutions/). 구간의 마지막 풍선을 고르는 구간 DP. O(n³).
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    val n = vals.size
    val best = Array(n) { IntArray(n) }
    for (length in 2 until n) {
        for (left in 0 until n - length) {
            val right = left + length
            Drill.visit(left, right)
            var top = 0
            for (k in left + 1 until right) {
                val score = best[left][k] + best[k][right] + vals[left] * vals[k] * vals[right]
                if (score > top) { top = score; Drill.compare(k, top) }
            }
            best[left][right] = top
            Drill.write(left, top)
        }
    }
    return best[0][n - 1]
}
