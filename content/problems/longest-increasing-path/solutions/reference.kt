// 검증용 정답 (§6.1 solutions/). 위상 순서로 층을 벗긴다 — 재귀 없이.
fun longestIncreasingPath(grid: Array<IntArray>): Int {
    val rows = grid.size
    if (rows == 0 || grid[0].isEmpty()) return 0
    val cols = grid[0].size
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    val lower = IntArray(rows * cols)
    for (r in 0 until rows) for (c in 0 until cols) for (k in 0 until 4) {
        val nr = r + dr[k]; val nc = c + dc[k]
        if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] < grid[r][c]) lower[r * cols + c] += 1
    }
    var layer = IntArray(rows * cols)
    var size = 0
    for (i in 0 until rows * cols) if (lower[i] == 0) { layer[size] = i; size += 1 }
    var next = IntArray(rows * cols)
    var length = 0
    while (size > 0) {
        length += 1
        var nextSize = 0
        for (t in 0 until size) {
            val cell = layer[t]
            val r = cell / cols; val c = cell % cols
            Drill.visit(cell, length)
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] > grid[r][c]) {
                    val id = nr * cols + nc
                    lower[id] -= 1
                    if (lower[id] == 0) { next[nextSize] = id; nextSize += 1 }
                }
            }
        }
        val swap = layer; layer = next; next = swap
        size = nextSize
    }
    return length
}
