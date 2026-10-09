// kind: WRONG_BRANCH
// 칸을 처음 덱에 넣을 때 거리를 확정한다. 무게 1 로 먼저 닿은 칸에 나중에 무게 0 의 더 싼 길이 와도 고치지 않는다.
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dr = intArrayOf(0, 0, 0, 1, -1); val dc = intArrayOf(0, 1, -1, 0, 0)
    val dist = IntArray(m * n) { Int.MAX_VALUE }
    val seen = BooleanArray(m * n)
    dist[0] = 0; seen[0] = true
    val queue = ArrayDeque<Int>(); queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / n; val c = cell % n
        for (sign in 1..4) {
            val a = r + dr[sign]; val b = c + dc[sign]
            if (a < 0 || a >= m || b < 0 || b >= n) continue
            val next = a * n + b
            if (seen[next]) continue
            val w = if (grid[r][c] == sign) 0 else 1
            seen[next] = true; dist[next] = dist[cell] + w
            if (w == 0) queue.addFirst(next) else queue.addLast(next)
        }
    }
    return dist[m * n - 1]
}
