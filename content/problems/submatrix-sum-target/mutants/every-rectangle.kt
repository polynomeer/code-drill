// kind: PERFORMANCE
// 모든 직사각형의 합을 누적합으로 하나씩 잰다. 직사각형 수가 행² × 열² 이다.
fun submatrixSumTarget(grid: Array<IntArray>, target: Int): Int {
    val rows = grid.size; val cols = grid[0].size
    val prefix = Array(rows + 1) { LongArray(cols + 1) }
    for (r in 0 until rows) for (c in 0 until cols) prefix[r + 1][c + 1] = grid[r][c] + prefix[r][c + 1] + prefix[r + 1][c] - prefix[r][c]
    var count = 0
    for (top in 0 until rows) for (bottom in top + 1..rows) {
        Drill.compare(top, bottom)
        for (left in 0 until cols) for (right in left + 1..cols) {
            val sum = prefix[bottom][right] - prefix[top][right] - prefix[bottom][left] + prefix[top][left]
            if (sum == target.toLong()) count += 1
        }
    }
    return count
}
