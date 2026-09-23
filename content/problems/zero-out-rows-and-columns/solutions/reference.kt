// 검증용 정답 (§6.1 solutions/). 첫 행·첫 열을 표시판으로 쓰고, 그 둘은 맨 마지막에 처리한다.
fun zeroOutRowsAndColumns(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    var firstRowHasZero = false
    var firstColumnHasZero = false
    for (c in 0 until cols) if (grid[0][c] == 0) firstRowHasZero = true
    for (r in 0 until rows) if (grid[r][0] == 0) firstColumnHasZero = true
    for (r in 1 until rows) for (c in 1 until cols) if (grid[r][c] == 0) { grid[r][0] = 0; grid[0][c] = 0 }
    for (r in 1 until rows) for (c in 1 until cols) {
        if (grid[r][0] == 0 || grid[0][c] == 0) { grid[r][c] = 0; Drill.write(r * cols + c, 0) }
    }
    if (firstRowHasZero) for (c in 0 until cols) grid[0][c] = 0
    if (firstColumnHasZero) for (r in 0 until rows) grid[r][0] = 0
    return grid
}
