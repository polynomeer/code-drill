// kind: WRONG_ALGORITHM
// 제자리에서 바꾼다. 먼저 바뀐 칸이 아직 안 바뀐 칸의 이웃 수에 들어간다.
fun lifeStep(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(rows) { grid[it].copyOf() }
    for (r in 0 until rows) for (c in 0 until cols) {
        var alive = 0
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && out[nr][nc] == 1) alive += 1
        }
        out[r][c] = if (out[r][c] == 1) (if (alive == 2 || alive == 3) 1 else 0) else (if (alive == 3) 1 else 0)
    }
    return out
}
