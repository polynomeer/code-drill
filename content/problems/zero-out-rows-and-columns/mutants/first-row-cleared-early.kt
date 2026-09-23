// kind: MISSING_EDGE_CASE
// 표시판으로 쓴 첫 행과 첫 열을 본문보다 먼저 지운다. 표시가 사라지기 전에 번진다.
fun zeroOutRowsAndColumns(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    var firstRowHasZero = false
    var firstColumnHasZero = false
    for (c in 0 until cols) if (grid[0][c] == 0) firstRowHasZero = true
    for (r in 0 until rows) if (grid[r][0] == 0) firstColumnHasZero = true
    for (r in 1 until rows) for (c in 1 until cols) if (grid[r][c] == 0) { grid[r][0] = 0; grid[0][c] = 0 }
    if (firstRowHasZero) for (c in 0 until cols) grid[0][c] = 0
    if (firstColumnHasZero) for (r in 0 until rows) grid[r][0] = 0
    for (r in 1 until rows) for (c in 1 until cols) if (grid[r][0] == 0 || grid[0][c] == 0) grid[r][c] = 0
    return grid
}
