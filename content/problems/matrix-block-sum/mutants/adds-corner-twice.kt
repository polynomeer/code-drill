// kind: WRONG_BRANCH
// 포함·배제에서 왼쪽 위 모서리를 빼지 않고 더한다.
fun blockSum(grid: Array<IntArray>, k: Int): Array<IntArray> {
    val rows = grid.size; val cols = grid[0].size
    val pre = Array(rows + 1) { IntArray(cols + 1) }
    for (r in 0 until rows) { var acc = 0; for (c in 0 until cols) { acc += grid[r][c]; pre[r + 1][c + 1] = pre[r][c + 1] + acc } }
    return Array(rows) { r ->
        val r1 = maxOf(0, r - k); val r2 = minOf(rows, r + k + 1)
        IntArray(cols) { c ->
            val c1 = maxOf(0, c - k); val c2 = minOf(cols, c + k + 1)
            pre[r2][c2] - pre[r1][c2] - pre[r2][c1] - pre[r1][c1]
        }
    }
}
