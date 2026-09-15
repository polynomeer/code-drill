// kind: MISSING_EDGE_CASE
// 0 을 만나도 위로 이어진 개수를 되돌리지 않는다. 끊긴 열을 이어진 것으로 본다.
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val heights = IntArray(cols)
    var best = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) if (grid[r][c] == 1) heights[c] += 1
        val stack = ArrayDeque<Int>()
        for (i in 0..cols) {
            val current = if (i == cols) 0 else heights[i]
            while (stack.isNotEmpty() && heights[stack.last()] >= current) {
                val h = heights[stack.removeLast()]
                val left = if (stack.isEmpty()) 0 else stack.last() + 1
                best = maxOf(best, h * (i - left))
            }
            if (i < cols) stack.addLast(i)
        }
    }
    return best
}
