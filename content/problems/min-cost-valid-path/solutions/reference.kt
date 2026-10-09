// 검증용 정답 (§6.1 solutions/). 0-1 BFS — 화살표 방향은 무게 0 이라 덱 앞에, 아니면 무게 1 이라 덱 뒤에 넣는다.
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size
    val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, 1, -1)
    val dc = intArrayOf(0, 1, -1, 0, 0)
    val dist = IntArray(m * n) { Int.MAX_VALUE }
    dist[0] = 0
    val queue = ArrayDeque<Int>()
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        val r = cell / n; val c = cell % n
        Drill.visit(cell, dist[cell])
        for (sign in 1..4) {
            val a = r + dr[sign]; val b = c + dc[sign]
            if (a < 0 || a >= m || b < 0 || b >= n) continue
            val w = if (grid[r][c] == sign) 0 else 1
            val next = a * n + b
            if (dist[cell] + w < dist[next]) {
                dist[next] = dist[cell] + w
                if (w == 0) queue.addFirst(next) else queue.addLast(next)
            }
        }
    }
    return dist[m * n - 1]
}
