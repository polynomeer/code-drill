// 검증용 정답 (§6.1 solutions/). 가장자리에서 시작하는 최소 힙.
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    val seen = BooleanArray(rows * cols)
    val heap = java.util.PriorityQueue<LongArray>(compareBy { it[0] })
    fun push(level: Int, r: Int, c: Int) { if (!seen[r * cols + c]) { seen[r * cols + c] = true; heap.add(longArrayOf(level.toLong(), r.toLong(), c.toLong())) } }
    for (r in 0 until rows) { push(heights[r][0], r, 0); push(heights[r][cols - 1], r, cols - 1) }
    for (c in 0 until cols) { push(heights[0][c], 0, c); push(heights[rows - 1][c], rows - 1, c) }
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (heap.isNotEmpty()) {
        val top = heap.poll()
        val level = top[0].toInt(); val r = top[1].toInt(); val c = top[2].toInt()
        Drill.compare(r, c)
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
            if (heights[nr][nc] < level) { water += level - heights[nr][nc]; Drill.write(0, water.toInt()) }
            push(maxOf(level, heights[nr][nc]), nr, nc)
        }
    }
    return water.toInt()
}
