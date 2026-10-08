// kind: MISSING_EDGE_CASE
// 메모를 붙인 재귀로 따라간다. 갈 곳이 하나뿐인 긴 줄에서는 깊이가 칸 수만큼이라 스택이 넘친다.
fun longestIncreasingPath(grid: Array<IntArray>): Int {
    val rows = grid.size
    if (rows == 0 || grid[0].isEmpty()) return 0
    val cols = grid[0].size
    val memo = Array(rows) { IntArray(cols) }
    fun go(r: Int, c: Int): Int {
        if (memo[r][c] != 0) return memo[r][c]
        var best = 1
        for ((dr, dc) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc] > grid[r][c]) best = maxOf(best, 1 + go(nr, nc))
        }
        memo[r][c] = best
        return best
    }
    var answer = 0
    for (r in 0 until rows) for (c in 0 until cols) answer = maxOf(answer, go(r, c))
    return answer
}
