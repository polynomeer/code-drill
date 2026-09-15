// kind: PERFORMANCE
// 행마다 히스토그램을 만든 뒤 열의 모든 쌍에 대해 그 사이의 최소 높이로 넓이를 잰다. O(rows × cols²).
fun maximalRectangle(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val heights = IntArray(cols)
    var best = 0
    for (r in 0 until rows) {
        for (c in 0 until cols) heights[c] = if (grid[r][c] == 1) heights[c] + 1 else 0
        for (left in 0 until cols) {
            var lowest = heights[left]
            for (right in left until cols) {
                Drill.compare(left, right)
                lowest = minOf(lowest, heights[right])
                if (lowest == 0) break
                best = maxOf(best, lowest * (right - left + 1))
            }
        }
    }
    return best
}
