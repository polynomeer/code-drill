// kind: WRONG_ALGORITHM
// 수열의 첫 값과 같은 첫 칸에서만 시작한다. 다른 시작점을 보지 않는다.
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val used = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        if (k == sequence.size - 1) return true
        used[r][c] = true
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && !used[nr][nc] && walk(nr, nc, k + 1)) { used[r][c] = false; return true }
        }
        used[r][c] = false
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] == sequence[0]) return if (walk(r, c, 0)) 1 else 0
    return 0
}
