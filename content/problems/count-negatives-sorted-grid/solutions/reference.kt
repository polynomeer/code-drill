// 검증용 정답 (§6.1 solutions/). 오른쪽 위에서 시작하는 계단.
fun countNegatives(grid: Array<IntArray>): Int {
    val cols = grid[0].size
    var count = 0
    var col = cols - 1
    for (r in grid.indices) {
        while (col >= 0 && grid[r][col] < 0) { Drill.compare(r, col); col -= 1 }
        count += cols - 1 - col
        Drill.write(r, count)
    }
    return count
}
