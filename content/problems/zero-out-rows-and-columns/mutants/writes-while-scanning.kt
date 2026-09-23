// kind: WRONG_ALGORITHM
// 0 을 만나는 자리에서 바로 행과 열을 채운다. 채운 0 이 다시 기준이 되어 번진다.
fun zeroOutRowsAndColumns(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    for (r in 0 until rows) for (c in 0 until cols) {
        if (grid[r][c] == 0) {
            for (cc in 0 until cols) grid[r][cc] = 0
            for (rr in 0 until rows) grid[rr][c] = 0
        }
    }
    return grid
}
