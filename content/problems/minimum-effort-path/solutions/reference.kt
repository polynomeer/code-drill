// 검증용 정답 (§6.1 solutions/). 임계값 이분 탐색 + BFS.
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val seen = BooleanArray(rows * cols)
    val queue = IntArray(rows * cols)
    fun reachable(limit: Int): Boolean {
        seen.fill(false)
        var head = 0; var tail = 0
        queue[tail++] = 0; seen[0] = true
        while (head < tail) {
            val cell = queue[head++]
            if (cell == rows * cols - 1) return true
            val r = cell / cols; val c = cell % cols
            val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                val next = nr * cols + nc
                if (!seen[next] && kotlin.math.abs(heights[nr][nc] - heights[r][c]) <= limit) { seen[next] = true; queue[tail++] = next }
            }
        }
        return false
    }
    var lo = 0; var hi = 1_000_000
    while (lo < hi) {
        val mid = (lo + hi) / 2
        Drill.compare(lo, hi)
        if (reachable(mid)) hi = mid else lo = mid + 1
    }
    Drill.write(0, lo)
    return lo
}
