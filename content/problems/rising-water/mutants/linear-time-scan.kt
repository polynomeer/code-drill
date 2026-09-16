// kind: PERFORMANCE
// 시각을 0 부터 하나씩 올리며 매번 BFS 를 돌린다. 이분 탐색이 없어 O(n⁴).
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    fun reachable(t: Int): Boolean {
        if (grid[0][0] > t) return false
        val seen = BooleanArray(n * n)
        val queue = ArrayDeque<Int>()
        queue.addLast(0); seen[0] = true
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (cell == n * n - 1) return true
            val r = cell / n; val c = cell % n
            for (k in 0 until 4) {
                val nr = r + dr[k]; val nc = c + dc[k]
                if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
                val idx = nr * n + nc
                Drill.compare(t, grid[nr][nc])
                if (!seen[idx] && grid[nr][nc] <= t) { seen[idx] = true; queue.addLast(idx) }
            }
        }
        return false
    }
    var t = 0
    while (!reachable(t)) t += 1
    return t
}
