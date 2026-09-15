// kind: OFF_BY_ONE
// 개수가 k 를 넘는 가장 작은 값을 찾는다. k 이상이어야 한다.
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var lo = grid[0][0]; var hi = grid[n - 1][n - 1]
    while (lo < hi) {
        val mid = Math.floorDiv(lo + hi, 2)
        var count = 0; var col = n - 1
        for (row in grid) { while (col >= 0 && row[col] > mid) col -= 1; count += col + 1 }
        if (count <= k) lo = mid + 1 else hi = mid
    }
    return lo
}
