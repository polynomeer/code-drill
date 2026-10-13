// kind: PERFORMANCE
// 오른쪽 아래 모서리마다 왼쪽으로 하나씩 늘려 가며 높이의 최솟값을 더한다. 맞지만, 행마다 n² 이다.
fun countOnesSubmatrices(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val height = IntArray(n)
    var total = 0L
    for (i in 0 until m) {
        for (j in 0 until n) height[j] = if (grid[i][j] == 1) height[j] + 1 else 0
        for (j in 0 until n) {
            var low = Int.MAX_VALUE
            var k = j
            while (k >= 0 && height[k] > 0) { low = minOf(low, height[k]); total += low; k -= 1 }
        }
    }
    return (total % 1_000_000_007).toInt()
}
