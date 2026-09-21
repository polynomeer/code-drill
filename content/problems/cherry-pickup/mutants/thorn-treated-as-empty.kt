// kind: MISSING_EDGE_CASE
// 가시를 빈 칸으로 본다. 못 지나가는 길로 간다.
fun cherryPickup(grid: Array<IntArray>): Int {
    val n = grid.size
    val neg = Int.MIN_VALUE / 2
    var dp = Array(n) { IntArray(n) { neg } }
    dp[0][0] = maxOf(0, grid[0][0])
    for (t in 1 until 2 * n - 1) {
        val next = Array(n) { IntArray(n) { neg } }
        val lo = maxOf(0, t - n + 1); val hi = minOf(n - 1, t)
        for (r1 in lo..hi) { val c1 = t - r1
            for (r2 in lo..hi) { val c2 = t - r2
                var best = neg
                for (p1 in r1 - 1..r1) for (p2 in r2 - 1..r2) if (p1 >= 0 && p2 >= 0 && dp[p1][p2] > best) best = dp[p1][p2]
                if (best == neg) continue
                next[r1][r2] = best + maxOf(0, grid[r1][c1]) + (if (r1 != r2) maxOf(0, grid[r2][c2]) else 0)
            }
        }
        dp = next
    }
    return maxOf(0, dp[n - 1][n - 1])
}
