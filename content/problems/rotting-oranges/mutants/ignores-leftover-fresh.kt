// kind: MISSING_EDGE_CASE
// 끝까지 썩지 않은 오렌지가 남아도 걸린 시간을 돌려준다.
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    for (r in 0 until rows) for (c in 0 until cols) if (state[r][c] == 2) queue.addLast(r * cols + c)
    var minutes = 0
    while (queue.isNotEmpty()) {
        var spread = false
        repeat(queue.size) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2; spread = true
                queue.addLast(nr * cols + nc)
            }
        }
        if (spread) minutes += 1
    }
    return minutes
}
