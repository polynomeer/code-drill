// kind: PERFORMANCE
// 칸마다 메모 없이 모든 오르막 경로를 따라간다. 경로의 가짓수가 폭발한다.
fun longestIncreasingPath(grid: Array<IntArray>): Int {
    val rows = grid.size
    if (rows == 0 || grid[0].isEmpty()) return 0
    val cols = grid[0].size
    val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    var best = 0
    for (sr in 0 until rows) for (sc in 0 until cols) {
        val stack = java.util.ArrayDeque<IntArray>()
        stack.push(intArrayOf(sr, sc, 1))
        while (stack.isNotEmpty()) {
            val (r, c, len) = stack.pop()
            Drill.compare(r * cols + c, len)
            if (len > best) best = len
            for ((dr, dc) in dirs) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] > grid[r][c]) stack.push(intArrayOf(nr, nc, len + 1))
            }
        }
    }
    return best
}
