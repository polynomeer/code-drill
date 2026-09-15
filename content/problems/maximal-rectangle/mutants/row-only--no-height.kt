// kind: WRONG_ALGORITHM
// 행마다 연속한 1 의 길이만 잰다. 세로로 뻗는 직사각형을 보지 못한다.
fun maximalRectangle(grid: Array<IntArray>): Int {
    var best = 0
    for (row in grid) {
        var run = 0
        for (v in row) { run = if (v == 1) run + 1 else 0; best = maxOf(best, run) }
    }
    return best
}
