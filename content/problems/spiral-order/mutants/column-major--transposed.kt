// kind: WRONG_ALGORITHM
// 열 순서대로 읽는다.
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0
    for (c in 0 until cols) {
        for (r in 0 until rows) {
            out[head] = grid[r][c]
            head += 1
        }
    }
    return out
}
