// 검증용 정답 (§6.1 solutions/). 행 쌍을 고르고, 그 사이 열 합의 배열에서 누적합 해시맵으로 센다.
fun submatrixSumTarget(grid: Array<IntArray>, target: Int): Int {
    val rows = grid.size
    val cols = grid[0].size
    val prefix = Array(rows + 1) { LongArray(cols + 1) }
    for (r in 0 until rows) for (c in 0 until cols) {
        prefix[r + 1][c + 1] = grid[r][c] + prefix[r][c + 1] + prefix[r + 1][c] - prefix[r][c]
    }
    var count = 0L
    val seen = HashMap<Long, Int>()
    for (top in 0 until rows) for (bottom in top + 1..rows) {
        seen.clear()
        seen[0L] = 1
        for (c in 1..cols) {
            val running = prefix[bottom][c] - prefix[top][c]
            count += seen[running - target] ?: 0
            seen[running] = (seen[running] ?: 0) + 1
        }
        Drill.write(top * rows + bottom, count.toInt())
    }
    return count.toInt()
}
