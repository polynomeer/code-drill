// kind: WRONG_ALGORITHM
// 상하좌우만 움직인다. 대각선이 없어 길이 길어지거나 끊긴다.
fun shortestClearPath(grid: Array<IntArray>): Int {
    val n = grid.size
    if (grid[0][0] != 0 || grid[n - 1][n - 1] != 0) return -1
    val dist = IntArray(n * n); val queue = IntArray(n * n); var head = 0; var tail = 0
    dist[0] = 1; queue[tail++] = 0
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (head < tail) {
        val cell = queue[head++]; val r = cell / n; val c = cell % n
        if (cell == n * n - 1) return dist[cell]
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until n || nc !in 0 until n || grid[nr][nc] != 0) continue
            val next = nr * n + nc
            if (dist[next] == 0) { dist[next] = dist[cell] + 1; queue[tail++] = next }
        }
    }
    return -1
}
