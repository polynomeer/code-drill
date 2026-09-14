// kind: WRONG_BRANCH
// 대각선으로도 퍼진다고 본다. 상하좌우만이다.
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    var fresh = 0
    for (r in 0 until rows) for (c in 0 until cols) {
        if (state[r][c] == 2) queue.addLast(r * cols + c) else if (state[r][c] == 1) fresh += 1
    }
    var minutes = 0
    while (queue.isNotEmpty() && fresh > 0) {
        minutes += 1
        repeat(queue.size) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2; fresh -= 1
                queue.addLast(nr * cols + nc)
            }
        }
    }
    return if (fresh == 0) minutes else -1
}
