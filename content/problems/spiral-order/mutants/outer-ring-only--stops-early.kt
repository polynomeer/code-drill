// kind: MISSING_EDGE_CASE
// 바깥 한 바퀴만 돌고 안쪽으로 들어가지 않는다.
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0
    var top = 0
    var bottom = rows - 1
    var left = 0
    var right = cols - 1
    if (head < out.size) {
        for (c in left..right) { out[head] = grid[top][c]; head += 1 }
        top += 1
        for (r in top..bottom) { out[head] = grid[r][right]; head += 1 }
        right -= 1
        if (top <= bottom) {
            for (c in right downTo left) { out[head] = grid[bottom][c]; head += 1 }
            bottom -= 1
        }
        if (left <= right) {
            for (r in bottom downTo top) { out[head] = grid[r][left]; head += 1 }
            left += 1
        }
    }
    return out
}
