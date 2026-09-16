// kind: WRONG_BRANCH
// 비용 0 으로 간 칸도 큐의 뒤에 넣는다. 큐가 비용순이 아니게 되어 나중에 더 싼 길을 놓친다.
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val cost = IntArray(rows * cols) { Int.MAX_VALUE }
    val seen = BooleanArray(rows * cols)
    val queue = ArrayDeque<Int>()
    cost[0] = grid[0][0]; queue.addLast(0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        if (seen[cell]) continue
        seen[cell] = true
        val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val nd = cost[cell] + grid[nr][nc]; val idx = nr * cols + nc
            if (nd < cost[idx]) { cost[idx] = nd; queue.addLast(idx) }
        }
    }
    return cost[rows * cols - 1]
}
