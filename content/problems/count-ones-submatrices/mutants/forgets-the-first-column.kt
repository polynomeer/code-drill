// kind: OFF_BY_ONE
// 왼쪽에 더 낮은 열이 없을 때 h × j 로 센다. 0 번 열까지 j + 1 개의 열이 있다.
fun countOnesSubmatrices(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val height = IntArray(n); val sums = LongArray(n); val stack = IntArray(n)
    var total = 0L
    for (i in 0 until m) {
        for (j in 0 until n) height[j] = if (grid[i][j] == 1) height[j] + 1 else 0
        var size = 0
        for (j in 0 until n) {
            while (size > 0 && height[stack[size - 1]] >= height[j]) size -= 1
            sums[j] = if (size > 0) { val p = stack[size - 1]; sums[p] + height[j].toLong() * (j - p) } else height[j].toLong() * j
            stack[size++] = j; total += sums[j]
        }
    }
    return (total % 1_000_000_007).toInt()
}
