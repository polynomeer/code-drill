// kind: MISSING_EDGE_CASE
// 상하좌우만 이웃으로 센다. 대각선을 잊었다.
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val next = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] == 1) alive += 1
        }
        next[r][c] = if (grid[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
    }
    return next
}
