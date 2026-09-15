// kind: OFF_BY_ONE
// 마지막 풍선 후보를 right-1 앞까지만 본다. 구간의 끝 풍선을 마지막에 터뜨리는 경우가 빠진다.
fun burstBalloons(nums: IntArray): Int {
    val vals = IntArray(nums.size + 2) { 1 }
    for (i in nums.indices) vals[i + 1] = nums[i]
    val n = vals.size
    val best = Array(n) { IntArray(n) }
    for (length in 2 until n) for (left in 0 until n - length) {
        val right = left + length
        var top = 0
        for (k in left + 1 until maxOf(left + 2, right - 1)) top = maxOf(top, best[left][k] + best[k][right] + vals[left] * vals[k] * vals[right])
        best[left][right] = top
    }
    return best[0][n - 1]
}
