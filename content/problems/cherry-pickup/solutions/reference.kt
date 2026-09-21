// 검증용 정답 (§6.1 solutions/). 걸음 수마다 (r1, r2) 표.
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    if (grid[0][0] < 0) return 0
    val neg = Int.MIN_VALUE / 2
    var dp = Array(n) { IntArray(n) { neg } }
    dp[0][0] = grid[0][0]
    for (t in 1 until 2 * n - 1) {
        val next = Array(n) { IntArray(n) { neg } }
        val lo = maxOf(0, t - n + 1); val hi = minOf(n - 1, t)
        for (r1 in lo..hi) {
            val c1 = t - r1
            if (grid[r1][c1] < 0) continue
            for (r2 in lo..hi) {
                val c2 = t - r2
                if (grid[r2][c2] < 0) continue
                var best = neg
                for (p1 in r1 - 1..r1) for (p2 in r2 - 1..r2) if (p1 >= 0 && p2 >= 0 && dp[p1][p2] > best) best = dp[p1][p2]
                if (best == neg) continue
                Drill.compare(r1, r2)
                next[r1][r2] = best + grid[r1][c1] + (if (r1 != r2) grid[r2][c2] else 0)
            }
        }
        dp = next
        Drill.write(t, maxOf(0, dp[minOf(n - 1, t)][minOf(n - 1, t)]))
    }
    return maxOf(0, dp[n - 1][n - 1])
}
