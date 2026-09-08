// kind: WRONG_ALGORITHM
// 나선이 아니라 행 순서대로 읽는다.
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[head] = grid[r][c]
            head += 1
        }
    }
    return out
}
