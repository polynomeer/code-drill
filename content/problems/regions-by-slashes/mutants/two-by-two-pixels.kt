// kind: WRONG_ALGORITHM
// 칸을 2×2 픽셀로 그려 빈 픽셀의 덩어리를 센다. 대각선 사이의 한 픽셀짜리 틈이 막혀 영역을 놓친다.
fun regionsBySlashes(grid: Array<String>): Int {
    val n = grid.size
    val size = 2 * n
    val wall = Array(size) { BooleanArray(size) }
    for (r in 0 until n) for (c in 0 until n) {
        when (grid[r][c]) {
            '/' -> { wall[2 * r][2 * c + 1] = true; wall[2 * r + 1][2 * c] = true }
            '\\' -> { wall[2 * r][2 * c] = true; wall[2 * r + 1][2 * c + 1] = true }
        }
    }
    var count = 0
    val stack = ArrayDeque<Int>()
    for (i in 0 until size) for (j in 0 until size) {
        if (wall[i][j]) continue
        count += 1
        wall[i][j] = true
        stack.addLast(i * size + j)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast(); val x = cur / size; val y = cur % size
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val a = x + dx; val b = y + dy
                if (a in 0 until size && b in 0 until size && !wall[a][b]) { wall[a][b] = true; stack.addLast(a * size + b) }
            }
        }
    }
    return count
}
