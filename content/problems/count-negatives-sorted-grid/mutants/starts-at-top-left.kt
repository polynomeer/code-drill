// kind: WRONG_ALGORITHM
// 왼쪽 위에서 시작해 음수인 동안 오른쪽으로 간다. 음수는 오른쪽 끝에 몰려 있어 거의 아무것도 세지 못한다.
fun countNegatives(grid: Array<IntArray>): Int {
    val cols = grid[0].size
    var count = 0
    var col = 0
    for (r in grid.indices) {
        while (col < cols && grid[r][col] < 0) col += 1
        count += col
    }
    return count
}
