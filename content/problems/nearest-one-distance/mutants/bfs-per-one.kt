// kind: PERFORMANCE
// 1 마다 따로 퍼뜨리고 가장 작은 거리를 고른다. 1 의 수 × 칸 수.
fun nearestOne(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val best = Array(rows) { IntArray(cols) { Int.MAX_VALUE } }
    val queue = IntArray(rows * cols)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    for (sr in 0 until rows) for (sc in 0 until cols) {
        if (grid[sr][sc] != 1) continue
        val seen = Array(rows) { IntArray(cols) { -1 } }
        var head = 0; var tail = 0
        seen[sr][sc] = 0; queue[tail++] = sr * cols + sc
        while (head < tail) {
            val cell = queue[head++]; val r = cell / cols; val c = cell % cols
            Drill.compare(sr * cols + sc, cell)
            if (seen[r][c] < best[r][c]) best[r][c] = seen[r][c]
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr in 0 until rows && nc in 0 until cols && seen[nr][nc] == -1) { seen[nr][nc] = seen[r][c] + 1; queue[tail++] = nr * cols + nc }
            }
        }
    }
    return best
}
