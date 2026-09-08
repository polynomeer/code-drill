// kind: MISSING_EDGE_CASE
// 행과 열만 바꾸고 뒤집지 않는다.
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(cols) { IntArray(rows) }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[c][r] = grid[r][c]
        }
    }
    return out
}
