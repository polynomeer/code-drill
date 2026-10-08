// kind: WRONG_ALGORITHM
// 한 행 안의 구간만 센다. 여러 행에 걸친 직사각형도 부분 행렬이다.
fun submatrixSumTarget(grid: Array<IntArray>, target: Int): Int {
    var count = 0
    for (row in grid) {
        val seen = HashMap<Long, Int>(); seen[0L] = 1
        var running = 0L
        for (v in row) { running += v; count += seen[running - target] ?: 0; seen[running] = (seen[running] ?: 0) + 1 }
    }
    return count
}
