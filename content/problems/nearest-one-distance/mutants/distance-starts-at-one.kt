// kind: OFF_BY_ONE
// 1 인 칸에도 1 을 적는다. 자기 자신까지는 0 걸음이다.
fun nearestOne(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val dist = Array(rows) { IntArray(cols) { -1 } }
    val queue = IntArray(rows * cols)
    var head = 0; var tail = 0
    for (r in 0 until rows) for (c in 0 until cols) if (grid[r][c] == 1) { dist[r][c] = 1; queue[tail++] = r * cols + c }
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (head < tail) {
        val cell = queue[head++]; val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr in 0 until rows && nc in 0 until cols && dist[nr][nc] == -1) { dist[nr][nc] = dist[r][c] + 1; queue[tail++] = nr * cols + nc }
        }
    }
    return dist
}
