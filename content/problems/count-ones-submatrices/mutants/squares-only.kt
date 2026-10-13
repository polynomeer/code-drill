// kind: WRONG_ALGORITHM
// 모두 1 인 정사각형만 센다. 직사각형도 센다.
fun countOnesSubmatrices(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val side = Array(m) { IntArray(n) }
    var total = 0L
    for (i in 0 until m) for (j in 0 until n) if (grid[i][j] == 1) {
        side[i][j] = if (i == 0 || j == 0) 1 else 1 + minOf(side[i - 1][j], minOf(side[i][j - 1], side[i - 1][j - 1]))
        total += side[i][j]
    }
    return (total % 1_000_000_007).toInt()
}
