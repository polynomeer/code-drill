// kind: OFF_BY_ONE
// 출발 칸의 높이를 최댓값에 넣지 않는다. 출발 칸이 가장 높으면 틀린다.
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val best = IntArray(n * n) { Int.MAX_VALUE }
    val heap = java.util.PriorityQueue<IntArray>(compareBy { it[0] })
    best[0] = 0
    heap.add(intArrayOf(0, 0))
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val t = top[0]; val cell = top[1]
        if (t > best[cell]) continue
        if (cell == n * n - 1) return t
        val r = cell / n; val c = cell % n
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
            val idx = nr * n + nc; val nt = maxOf(t, grid[nr][nc])
            if (nt < best[idx]) { best[idx] = nt; heap.add(intArrayOf(nt, idx)) }
        }
    }
    return best[n * n - 1]
}
