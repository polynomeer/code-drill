// 검증용 정답 (§6.1 solutions/). DFS + 되돌리기.
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val used = Array(rows) { BooleanArray(cols) }
    val dr = intArrayOf(1, -1, 0, 0)
    val dc = intArrayOf(0, 0, 1, -1)
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        Drill.visit(r * cols + c, k)
        if (k == sequence.size - 1) { Drill.match(r * cols + c, k); return true }
        used[r][c] = true
        for (d in 0 until 4) {
            val nr = r + dr[d]
            val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !used[nr][nc] && walk(nr, nc, k + 1)) {
                used[r][c] = false
                return true
            }
        }
        used[r][c] = false
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
