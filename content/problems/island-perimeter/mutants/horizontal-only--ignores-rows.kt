// kind: MISSING_EDGE_CASE
// 좌우 이웃만 뺀다. 세로로 붙은 땅의 공유 변이 둘레에 남는다.
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            total += 4
            if (c > 0 && grid[r][c - 1] == 1) total -= 1
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1
        }
    }
    return total
}
