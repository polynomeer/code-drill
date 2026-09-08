// kind: OFF_BY_ONE
// 맞닿은 변을 양쪽에서 한 번씩 빼고 또 뺀다.
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            total += 4
            if (r > 0 && grid[r - 1][c] == 1) total -= 2
            if (r + 1 < rows && grid[r + 1][c] == 1) total -= 2
            if (c > 0 && grid[r][c - 1] == 1) total -= 2
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 2
        }
    }
    return total
}
