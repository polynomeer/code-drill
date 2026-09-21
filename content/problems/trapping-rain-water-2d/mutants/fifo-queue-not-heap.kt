// kind: WRONG_ALGORITHM
// 최소 힙 대신 보통 큐로 바깥부터 훑는다. 가장 낮은 고개부터 보지 않아 수위가 틀린다.
fun trappingRainWater2d(heights: Array<IntArray>): Int {
    val rows = heights.size; val cols = heights[0].size
    if (rows < 3 || cols < 3) return 0
    val seen = BooleanArray(rows * cols)
    val queue = ArrayDeque<IntArray>()
    fun push(level: Int, r: Int, c: Int) { if (!seen[r * cols + c]) { seen[r * cols + c] = true; queue.addLast(intArrayOf(level, r, c)) } }
    for (r in 0 until rows) { push(heights[r][0], r, 0); push(heights[r][cols - 1], r, cols - 1) }
    for (c in 0 until cols) { push(heights[0][c], 0, c); push(heights[rows - 1][c], rows - 1, c) }
    var water = 0L
    val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
    while (queue.isNotEmpty()) {
        val top = queue.removeFirst(); val level = top[0]; val r = top[1]; val c = top[2]
        for (k in 0 until 4) {
            val nr = r + dr[k]; val nc = c + dc[k]
            if (nr !in 0 until rows || nc !in 0 until cols || seen[nr * cols + nc]) continue
            if (heights[nr][nc] < level) water += level - heights[nr][nc]
            push(maxOf(level, heights[nr][nc]), nr, nc)
        }
    }
    return water.toInt()
}
