// kind: OFF_BY_ONE
// 간선 수를 답한다. 칸 수는 하나 더 많다.
fun shortestClearPath(grid: Array<IntArray>): Int {
    val n = grid.size
    if (grid[0][0] != 0 || grid[n - 1][n - 1] != 0) return -1
    val dist = IntArray(n * n) { -1 }; val queue = IntArray(n * n); var head = 0; var tail = 0
    dist[0] = 0; queue[tail++] = 0
    while (head < tail) {
        val cell = queue[head++]; val r = cell / n; val c = cell % n
        if (cell == n * n - 1) return dist[cell]
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr !in 0 until n || nc !in 0 until n || grid[nr][nc] != 0) continue
            val next = nr * n + nc
            if (dist[next] < 0) { dist[next] = dist[cell] + 1; queue[tail++] = next }
        }
    }
    return -1
}
