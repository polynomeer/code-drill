// kind: WRONG_BRANCH
// 물 칸도 땅으로 세어 격자 전체의 둘레를 낸다.
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            total += 4
            if (r > 0) total -= 1
            if (r + 1 < rows) total -= 1
            if (c > 0) total -= 1
            if (c + 1 < cols) total -= 1
        }
    }
    return total
}
