// kind: PERFORMANCE
// 행마다 처음부터 끝까지 세고, 값의 범위 전체를 하나씩 시험한다. 격자의 정렬을 쓰지 않는다.
fun kthSmallest(grid: Array<IntArray>, k: Int): Int {
    val n = grid.size
    var candidate = grid[0][0]
    while (true) {
        var count = 0
        for (row in grid) for (v in row) { Drill.compare(v, candidate); if (v <= candidate) count += 1 }
        if (count >= k) return candidate
        candidate += 1
    }
}
