// kind: PERFORMANCE
// 모든 경로를 깊이 우선으로 다 가 보고 가장 적게 부순 것을 고른다. 지수적이다.
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val seen = BooleanArray(rows * cols)
    var best = Int.MAX_VALUE
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    fun go(r: Int, c: Int, walls: Int) {
        Drill.visit(r * cols + c, grid[r][c])
        val w = walls + grid[r][c]
        if (w >= best) return
        if (r == rows - 1 && c == cols - 1) { best = w; return }
        seen[r * cols + c] = true
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || seen[nr * cols + nc]) continue
            go(nr, nc, w)
        }
        seen[r * cols + c] = false
    }
    go(0, 0, 0)
    return best
}
