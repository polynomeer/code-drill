// kind: WRONG_BRANCH
// 0 을 만나도 높이를 0 으로 되돌리지 않는다. 0 위의 1 이 아래로 이어진 것처럼 세어진다.
fun countOnesSubmatrices(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val height = IntArray(n); val sums = LongArray(n); val stack = IntArray(n)
    var total = 0L
    for (i in 0 until m) {
        for (j in 0 until n) height[j] = if (grid[i][j] == 1) height[j] + 1 else height[j]
        var size = 0
        for (j in 0 until n) {
            while (size > 0 && height[stack[size - 1]] >= height[j]) size -= 1
            sums[j] = if (size > 0) { val p = stack[size - 1]; sums[p] + height[j].toLong() * (j - p) } else height[j].toLong() * (j + 1)
            stack[size++] = j; total += sums[j]
        }
    }
    return (total % 1_000_000_007).toInt()
}
