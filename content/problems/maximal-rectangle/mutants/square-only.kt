// kind: WRONG_BRANCH
// 정사각형만 찾는다. 가로나 세로로 긴 직사각형을 놓친다.
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val side = Array(rows + 1) { IntArray(cols + 1) }
    var best = 0
    for (r in 1..rows) for (c in 1..cols) {
        if (grid[r - 1][c - 1] == 1) {
            side[r][c] = minOf(side[r - 1][c], side[r][c - 1], side[r - 1][c - 1]) + 1
            best = maxOf(best, side[r][c] * side[r][c])
        }
    }
    return best
}
