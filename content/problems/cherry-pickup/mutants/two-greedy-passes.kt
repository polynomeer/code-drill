// kind: WRONG_ALGORITHM
// 내려가며 최대로 줍고, 남은 격자에서 돌아오며 최대로 줍는다. 합이 최대가 아니다.
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    val neg = Int.MIN_VALUE / 2
    val g = Array(n) { grid[it].copyOf() }
    fun bestPath(): Int {
        val dp = Array(n) { IntArray(n) { neg } }
        if (g[0][0] < 0) return 0
        dp[0][0] = g[0][0]
        for (r in 0 until n) for (c in 0 until n) {
            if (g[r][c] < 0 || (r == 0 && c == 0)) continue
            val up = if (r > 0) dp[r - 1][c] else neg
            val left = if (c > 0) dp[r][c - 1] else neg
            val b = maxOf(up, left)
            if (b > neg) dp[r][c] = b + g[r][c]
        }
        if (dp[n - 1][n - 1] <= neg) return 0
        // 경로를 되짚어 체리를 지운다.
        var r = n - 1; var c = n - 1
        while (r > 0 || c > 0) {
            g[r][c] = 0
            val up = if (r > 0) dp[r - 1][c] else neg
            val left = if (c > 0) dp[r][c - 1] else neg
            if (up >= left) r -= 1 else c -= 1
        }
        g[0][0] = 0
        return dp[n - 1][n - 1]
    }
    val first = bestPath()
    if (first == 0 && g[n - 1][n - 1] < 0) return 0
    return first + bestPath()
}
