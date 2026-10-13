// 검증용 정답 (§6.1 solutions/). 열마다 위로 이어진 1 의 높이를 들고, 행마다 단조 스택으로 오른쪽 아래 모서리별 개수를 잇는다.
fun countOnesSubmatrices(grid: Array<IntArray>): Int {
    val m = grid.size; val n = grid[0].size
    val height = IntArray(n)
    val sums = LongArray(n)
    val stack = IntArray(n)
    var total = 0L
    for (i in 0 until m) {
        for (j in 0 until n) height[j] = if (grid[i][j] == 1) height[j] + 1 else 0
        var size = 0
        for (j in 0 until n) {
            while (size > 0 && height[stack[size - 1]] >= height[j]) size -= 1
            sums[j] = if (size > 0) {
                val p = stack[size - 1]
                sums[p] + height[j].toLong() * (j - p)
            } else {
                height[j].toLong() * (j + 1)
            }
            stack[size++] = j
            total += sums[j]
        }
        Drill.push(i)
    }
    return (total % 1_000_000_007).toInt()
}
