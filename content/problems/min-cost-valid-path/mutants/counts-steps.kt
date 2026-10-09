// kind: WRONG_ALGORITHM
// 칸 수를 센다(보통의 BFS). 화살표를 따라가는 걸음은 공짜다.
fun minCostValidPath(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val dist = IntArray(m * n) { -1 }
    dist[0] = 0
    val queue = ArrayDeque<Int>(); queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst(); val r = cell / n; val c = cell % n
        for ((a, b) in listOf(r to c + 1, r to c - 1, r + 1 to c, r - 1 to c)) {
            if (a < 0 || a >= m || b < 0 || b >= n || dist[a * n + b] >= 0) continue
            dist[a * n + b] = dist[cell] + 1; queue.addLast(a * n + b)
        }
    }
    return dist[m * n - 1]
}
