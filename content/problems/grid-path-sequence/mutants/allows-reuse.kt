// kind: MISSING_EDGE_CASE
// 같은 칸을 다시 밟는 것을 막지 않는다.
fun pathExists(grid: Array<IntArray>, sequence: IntArray): Int {
    if (sequence.isEmpty()) return 1
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    fun walk(r: Int, c: Int, k: Int): Boolean {
        if (grid[r][c] != sequence[k]) return false
        if (k == sequence.size - 1) return true
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && walk(nr, nc, k + 1)) return true
        }
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
