// kind: WRONG_ALGORITHM
// 썩은 오렌지를 하나씩 끝까지 퍼뜨린다. 여러 곳에서 동시에 퍼지는 것을 잊었다.
fun rottingMinutes(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val time = Array(rows) { IntArray(cols) { -1 } }
    var worst = 0
    for (r0 in 0 until rows) for (c0 in 0 until cols) {
        if (grid[r0][c0] != 2) continue
        val queue = ArrayDeque<Int>()
        time[r0][c0] = 0
        queue.addLast(r0 * cols + c0)
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until rows || nc !in 0 until cols || grid[nr][nc] != 1 || time[nr][nc] != -1) continue
                time[nr][nc] = time[r][c] + 1
                worst = maxOf(worst, time[nr][nc])
                queue.addLast(nr * cols + nc)
            }
        }
    }
    for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] == 1 && time[r][c] == -1) return -1
    return worst
}
