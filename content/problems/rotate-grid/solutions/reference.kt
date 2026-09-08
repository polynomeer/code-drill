// 검증용 정답 (§6.1 solutions/). 새 격자를 만들어 옮긴다.
//
// 제자리 회전은 정사각형에서만 되고, 여기서는 결과의 크기가 입력과 다르다. 크기를
// 먼저 정하고 옮기면 직사각형도 같은 코드로 끝난다.
fun rotate(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val out = Array(cols) { IntArray(rows) }

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            out[c][rows - 1 - r] = grid[r][c]
            Drill.write(c * rows + (rows - 1 - r), grid[r][c])
        }
    }
    return out
}
