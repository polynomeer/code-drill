// kind: OFF_BY_ONE
// 아래 행을 위 행보다 두 칸 아래부터 고른다. 한 행짜리 직사각형을 빠뜨린다.
fun submatrixSumTarget(grid: Array<IntArray>, target: Int): Int {
    val rows = grid.size; val cols = grid[0].size
    val prefix = Array(rows + 1) { LongArray(cols + 1) }
    for (r in 0 until rows) for (c in 0 until cols) prefix[r + 1][c + 1] = grid[r][c] + prefix[r][c + 1] + prefix[r + 1][c] - prefix[r][c]
    var count = 0L
    for (top in 0 until rows) for (bottom in top + 2..rows) {
        val seen = HashMap<Long, Int>(); seen[0L] = 1
        for (c in 1..cols) { val running = prefix[bottom][c] - prefix[top][c]; count += seen[running - target] ?: 0; seen[running] = (seen[running] ?: 0) + 1 }
    }
    return count.toInt()
}
