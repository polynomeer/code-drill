// kind: WRONG_ALGORITHM
// 매번 가장 낮은 이웃으로만 나아간다. 돌아가야 최댓값이 낮아지는 격자에서 틀린다.
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val seen = BooleanArray(n * n)
    var r = 0; var c = 0
    var peak = grid[0][0]
    seen[0] = true
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (!(r == n - 1 && c == n - 1)) {
        var bestR = -1; var bestC = -1
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= n || nc < 0 || nc >= n || seen[nr * n + nc]) continue
            if (bestR == -1 || grid[nr][nc] < grid[bestR][bestC]) { bestR = nr; bestC = nc }
        }
        if (bestR == -1) return n * n - 1
        r = bestR; c = bestC; seen[r * n + c] = true
        peak = maxOf(peak, grid[r][c])
    }
    return peak
}
