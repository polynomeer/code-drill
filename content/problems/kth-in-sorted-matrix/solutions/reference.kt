// 검증용 정답 (§6.1 solutions/). 값에 대한 이분 탐색 + 계단 세기.
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var lo = grid[0][0]
    var hi = grid[n - 1][n - 1]
    while (lo < hi) {
        val mid = Math.floorDiv(lo + hi, 2)
        Drill.compare(lo, hi)
        var count = 0
        var col = n - 1
        for (row in grid) {
            while (col >= 0 && row[col] > mid) col -= 1
            count += col + 1
        }
        Drill.visit(mid, count)
        if (count < k) lo = mid + 1 else hi = mid
    }
    return lo
}
