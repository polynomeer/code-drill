// kind: WRONG_ALGORITHM
// 화살표를 따라가다 막히면 그 자리에서 끝 쪽으로 화살표를 바꾼다. 지금 바꾸는 것이 가장 싼 길이라는 보장이 없다.
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, 1, -1); val dc = intArrayOf(0, 1, -1, 0, 0)
    val seen = BooleanArray(m * n)
    var r = 0; var c = 0; var cost = 0
    while (r != m - 1 || c != n - 1) {
        seen[r * n + c] = true
        val a = r + dr[grid[r][c]]; val b = c + dc[grid[r][c]]
        if (a in 0 until m && b in 0 until n && !seen[a * n + b]) { r = a; c = b; continue }
        cost += 1
        if (c < n - 1 && !seen[r * n + c + 1]) c += 1 else if (r < m - 1) r += 1 else c += 1
    }
    return cost
}
