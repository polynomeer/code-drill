// kind: WRONG_ALGORITHM
// 수고를 최댓값이 아니라 합으로 잰다. 보통의 최단 경로가 된다.
fun minimumEffortPath(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    val dist = LongArray(rows * cols) { Long.MAX_VALUE }
    val heap = java.util.PriorityQueue<Pair<Long, Int>>(compareBy { it.first })
    dist[0] = 0; heap.add(0L to 0)
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    var bestMax = 0
    val maxAlong = IntArray(rows * cols)
    while (heap.isNotEmpty()) {
        val (d, cell) = heap.poll()
        if (d > dist[cell]) continue
        if (cell == rows * cols - 1) { bestMax = maxAlong[cell]; break }
        val r = cell / cols; val c = cell % cols
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            val diff = kotlin.math.abs(heights[nr][nc] - heights[r][c])
            val next = nr * cols + nc
            if (d + diff < dist[next]) { dist[next] = d + diff; maxAlong[next] = maxOf(maxAlong[cell], diff); heap.add(dist[next] to next) }
        }
    }
    return bestMax
}
