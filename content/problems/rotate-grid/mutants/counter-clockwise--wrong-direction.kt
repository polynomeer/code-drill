// kind: WRONG_BRANCH
// 반시계 방향으로 돌린다.
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(cols) { IntArray(rows) }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[cols - 1 - c][r] = grid[r][c]
        }
    }
    return out
}
