// kind: OFF_BY_ONE
// 0 도 센다. 음수는 0 보다 작은 것이다.
fun countNegatives(grid: Array<IntArray>): Int {
    val cols = grid[0].size
    var count = 0
    var col = cols - 1
    for (r in grid.indices) {
        while (col >= 0 && grid[r][col] <= 0) col -= 1
        count += cols - 1 - col
    }
    return count
}
