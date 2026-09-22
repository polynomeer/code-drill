// kind: PERFORMANCE
// 모든 경로를 DFS 로 열거하며 최솟값을 갱신한다. 지수다.
fun shortestClearPath(grid: Array<IntArray>): Int {
    val n = grid.size
    if (grid[0][0] != 0 || grid[n - 1][n - 1] != 0) return -1
    var best = Int.MAX_VALUE
    val onPath = BooleanArray(n * n)
    fun go(r: Int, c: Int, depth: Int) {
        if (depth >= best) return
        if (r == n - 1 && c == n - 1) { best = depth; return }
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr !in 0 until n || nc !in 0 until n || grid[nr][nc] != 0 || onPath[nr * n + nc]) continue
            Drill.compare(nr, nc)
            onPath[nr * n + nc] = true; go(nr, nc, depth + 1); onPath[nr * n + nc] = false
        }
    }
    onPath[0] = true
    go(0, 0, 1)
    return if (best == Int.MAX_VALUE) -1 else best
}
