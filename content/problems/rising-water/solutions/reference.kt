// 검증용 정답 (§6.1 solutions/). 시각을 이분 탐색하고, 시각마다 BFS 로 닿는지 본다.
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    val seen = BooleanArray(n * n)
    val queue = IntArray(n * n)
    fun reachable(t: Int): Boolean {
        Drill.compare(t, grid[0][0])
        if (grid[0][0] > t) return false
        java.util.Arrays.fill(seen, false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            val r = cell / n; val c = cell % n
            Drill.visit(cell, grid[r][c])
            if (cell == n * n - 1) return true
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
                val idx = nr * n + nc
                if (!seen[idx] && grid[nr][nc] <= t) { seen[idx] = true; queue[tail++] = idx }
            }
        }
        return false
    }
    var lo = maxOf(grid[0][0], grid[n - 1][n - 1])
    var hi = n * n - 1
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (reachable(mid)) hi = mid else lo = mid + 1
    }
    return lo
}
