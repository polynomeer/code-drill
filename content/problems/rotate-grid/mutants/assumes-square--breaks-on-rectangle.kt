// kind: MISSING_EDGE_CASE
// 결과를 입력과 같은 크기로 잡는다. 정사각형에서는 맞고 직사각형에서 터진다.
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(rows) { IntArray(cols) }
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[c][rows - 1 - r] = grid[r][c]
        }
    }
    return out
}
