// kind: MISSING_EDGE_CASE
// 표시판으로 쓴 첫 열이 원래 0 을 갖고 있었는지 기억하지 않는다. 첫 열의 0 만 살아남는다.
fun zeroOutRowsAndColumns(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    var firstRowHasZero = false
    for (c in 0 until cols) if (grid[0][c] == 0) firstRowHasZero = true
    for (r in 1 until rows) for (c in 1 until cols) if (grid[r][c] == 0) { grid[r][0] = 0; grid[0][c] = 0 }
    for (r in 1 until rows) for (c in 1 until cols) if (grid[r][0] == 0 || grid[0][c] == 0) grid[r][c] = 0
    if (firstRowHasZero) for (c in 0 until cols) grid[0][c] = 0
    return grid
}
