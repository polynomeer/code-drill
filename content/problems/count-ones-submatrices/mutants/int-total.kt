// kind: MISSING_EDGE_CASE
// 개수를 Int 로 센다. 200 × 600 의 1 만 해도 수가 36 억이라 Int 를 넘는다.
fun countOnesSubmatrices(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val height = IntArray(n); val sums = IntArray(n); val stack = IntArray(n)
    var total = 0
    for (i in 0 until m) {
        for (j in 0 until n) height[j] = if (grid[i][j] == 1) height[j] + 1 else 0
        var size = 0
        for (j in 0 until n) {
            while (size > 0 && height[stack[size - 1]] >= height[j]) size -= 1
            sums[j] = if (size > 0) { val p = stack[size - 1]; sums[p] + height[j] * (j - p) } else height[j] * (j + 1)
            stack[size++] = j; total += sums[j]
        }
    }
    return total % 1_000_000_007
}
