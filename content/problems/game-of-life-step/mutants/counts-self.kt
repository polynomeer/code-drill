// kind: OFF_BY_ONE
// 자기 자신을 이웃에 넣어 센다. 산 칸의 이웃 수가 하나 많다.
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val next = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] == 1) alive += 1
        }
        next[r][c] = if (grid[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
    }
    return next
}
