// kind: WRONG_BRANCH
// 같은 값의 이웃으로도 간다고 센다. 같은 값끼리는 서로를 기다려 어느 쪽도 벗겨지지 않는다.
fun longestIncreasingPath(grid: Array<IntArray>): Int {
    val rows = grid.size
    if (rows == 0 || grid[0].isEmpty()) return 0
    val cols = grid[0].size
    val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    val lower = IntArray(rows * cols)
    for (r in 0 until rows) for (c in 0 until cols) for ((dr, dc) in dirs) {
        val nr = r + dr; val nc = c + dc
        if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] <= grid[r][c]) lower[r * cols + c] += 1
    }
    var layer = (0 until rows * cols).filter { lower[it] == 0 }
    var length = 0
    while (layer.isNotEmpty()) {
        length += 1
        val next = ArrayList<Int>()
        for (cell in layer) {
            val r = cell / cols; val c = cell % cols
            for ((dr, dc) in dirs) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] >= grid[r][c]) {
                    val id = nr * cols + nc
                    lower[id] -= 1
                    if (lower[id] == 0) next.add(id)
                }
            }
        }
        layer = next
    }
    return length
}
