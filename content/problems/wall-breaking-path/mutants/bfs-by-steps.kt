// kind: WRONG_ALGORITHM
// 걸음 수가 가장 적은 길을 찾고 그 길의 벽을 센다. 돌아가면 벽을 덜 부수는 경우를 놓친다.
fun minWallsToBreak(grid: Array<IntArray>): Int {
    val rows = grid.size; val cols = grid[0].size
    val walls = IntArray(rows * cols) { -1 }
    val queue = ArrayDeque<Int>()
    walls[0] = grid[0][0]; queue.addLast(0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue
            val idx = nr * cols + nc
            if (walls[idx] == -1) { walls[idx] = walls[cell] + grid[nr][nc]; queue.addLast(idx) }
        }
    }
    return walls[rows * cols - 1]
}
