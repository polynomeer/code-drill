// kind: WRONG_BRANCH
// 행마다 열 포인터를 되돌려 왼쪽부터 다시 센다 — 개수는 맞지만, 되돌린 뒤 mid 보다 큰 값을 세는 조건이 뒤집혔다.
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var lo = grid[0][0]; var hi = grid[n - 1][n - 1]
    while (lo < hi) {
        val mid = Math.floorDiv(lo + hi, 2)
        var count = 0
        for (row in grid) { var col = 0; while (col < n && row[col] < mid) col += 1; count += col }
        if (count < k) lo = mid + 1 else hi = mid
    }
    return lo
}
