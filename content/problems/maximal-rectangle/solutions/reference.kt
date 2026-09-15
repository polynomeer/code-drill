// 검증용 정답 (§6.1 solutions/). 행마다 히스토그램 + 단조 스택.
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val heights = IntArray(cols)
    val stack = IntArray(cols + 1)
    var best = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            heights[c] = if (grid[r][c] == 1) heights[c] + 1 else 0
            Drill.write(c, heights[c])
        }
        var top = 0
        for (i in 0..cols) {
            val current = if (i == cols) 0 else heights[i]
            while (top > 0 && heights[stack[top - 1]] >= current) {
                val h = heights[stack[--top]]
                Drill.pop(stack[top])
                val left = if (top == 0) 0 else stack[top - 1] + 1
                best = maxOf(best, h * (i - left))
            }
            if (i < cols) { stack[top++] = i; Drill.push(i) }
        }
    }
    return best
}
