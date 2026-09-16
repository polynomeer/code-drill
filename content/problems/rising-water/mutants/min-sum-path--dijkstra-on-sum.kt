// kind: WRONG_ALGORITHM
// 지나는 칸들의 높이 합이 가장 작은 길을 찾고 그 길의 최댓값을 답한다. 합과 최댓값은 다른 길을 고른다.
fun earliestSwimTime(grid: Array<IntArray>): Int {
    val n = grid.size
    val dist = LongArray(n * n) { Long.MAX_VALUE }
    val peak = IntArray(n * n)
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    dist[0] = grid[0][0].toLong(); peak[0] = grid[0][0]
    heap.add(longArrayOf(dist[0], 0L))
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll(); val d = top[0]; val cell = top[1].toInt()
        if (d > dist[cell]) continue
        val r = cell / n; val c = cell % n
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr < 0 || nr >= n || nc < 0 || nc >= n) continue
            val idx = nr * n + nc; val nd = d + grid[nr][nc]
            if (nd < dist[idx]) { dist[idx] = nd; peak[idx] = maxOf(peak[cell], grid[nr][nc]); heap.add(longArrayOf(nd, idx.toLong())) }
        }
    }
    return peak[n * n - 1]
}
