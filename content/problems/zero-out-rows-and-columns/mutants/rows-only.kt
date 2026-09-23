// kind: WRONG_BRANCH
// 0 이 있는 행만 지우고 열은 그대로 둔다.
fun zeroOutRowsAndColumns(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val zeroRow = BooleanArray(rows)
    for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] == 0) zeroRow[r] = true
    for (r in 0 until rows) if (zeroRow[r]) for (c in 0 until cols) grid[r][c] = 0
    return grid
}
