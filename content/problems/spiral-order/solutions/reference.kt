// 검증용 정답 (§6.1 solutions/). 네 경계를 안쪽으로 좁혀 간다.
//
// 한 줄만 남았을 때가 함정이다. 위쪽 줄을 읽고 top 을 내린 뒤에도 아래쪽 줄을 그대로
// 읽으면 같은 줄을 두 번 읽는다. `top <= bottom` 과 `left <= right` 가 그 자리를 막는다.
fun spiralOrder(grid: Array<IntArray>): IntArray {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = IntArray(rows * cols)
    var head = 0

    var top = 0
    var bottom = rows - 1
    var left = 0
    var right = cols - 1

    while (head < out.size) {
        for (c in left..right) {
            out[head] = grid[top][c]
            Drill.write(head, out[head])
            head += 1
        }
        top += 1

        for (r in top..bottom) {
            out[head] = grid[r][right]
            Drill.write(head, out[head])
            head += 1
        }
        right -= 1

        if (top <= bottom) {
            for (c in right downTo left) {
                out[head] = grid[bottom][c]
                Drill.write(head, out[head])
                head += 1
            }
            bottom -= 1
        }

        if (left <= right) {
            for (r in bottom downTo top) {
                out[head] = grid[r][left]
                Drill.write(head, out[head])
                head += 1
            }
            left += 1
        }
    }
    return out
}
