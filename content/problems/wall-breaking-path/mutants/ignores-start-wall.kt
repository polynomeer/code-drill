// kind: OFF_BY_ONE
// 출발 칸이 벽이어도 세지 않는다.
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val cost = IntArray(rows * cols) { Int.MAX_VALUE }
    val deque = java.util.ArrayDeque<Int>()
    cost[0] = 0; deque.addFirst(0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (deque.isNotEmpty()) {
        val cell = deque.pollFirst(); val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val nd = cost[cell] + grid[nr][nc]; val idx = nr * cols + nc
            if (nd < cost[idx]) { cost[idx] = nd; if (grid[nr][nc] == 0) deque.addFirst(idx) else deque.addLast(idx) }
        }
    }
    return cost[rows * cols - 1]
}
