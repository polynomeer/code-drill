// kind: OFF_BY_ONE
// 마지막으로 퍼뜨릴 것이 없던 회차까지 센다. 답이 1 크다.
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    var fresh = 0
    for (r in 0 until rows) for (c in 0 until cols) {
        if (state[r][c] == 2) queue.addLast(r * cols + c) else if (state[r][c] == 1) fresh += 1
    }
    if (fresh == 0) return 0
    var minutes = 0
    while (queue.isNotEmpty()) {
        minutes += 1
        repeat(queue.size) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2; fresh -= 1
                queue.addLast(nr * cols + nc)
            }
        }
    }
    return if (fresh == 0) minutes else -1
}
