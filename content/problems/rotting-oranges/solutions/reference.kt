// 검증용 정답 (§6.1 solutions/). 썩은 칸 전부에서 동시에 시작하는 BFS.
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val state = Array(rows) { grid[it].copyOf() }
    val queue = ArrayDeque<Int>()
    var fresh = 0
    for (r in 0 until rows) for (c in 0 until cols) {
        if (state[r][c] == 2) { queue.addLast(r * cols + c); Drill.enqueue(r * cols + c) }
        else if (state[r][c] == 1) fresh += 1
    }
    var minutes = 0
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty() && fresh > 0) {
        minutes += 1
        repeat(queue.size) {
            val cell = queue.removeFirst()
            Drill.dequeue(cell)
            val r = cell / cols
            val c = cell % cols
            for (k in 0 until 4) {
                val nr = r + dr[k]
                val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols || state[nr][nc] != 1) continue
                state[nr][nc] = 2
                fresh -= 1
                Drill.write(nr * cols + nc, minutes)
                queue.addLast(nr * cols + nc)
            }
        }
    }
    return if (fresh == 0) minutes else -1
}
