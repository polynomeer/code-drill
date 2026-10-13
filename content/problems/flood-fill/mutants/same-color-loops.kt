// kind: MISSING_EDGE_CASE
// 새 색이 원래 색과 같은 경우를 보지 않고 방문 표시도 없다. 칠해도 색이 그대로라 같은 칸을 끝없이 다시 넣는다.
fun floodFill(grid: Array<IntArray>, r: Int, c: Int, color: Int): Array<IntArray> {
    val g = Array(grid.size) { grid[it].copyOf() }
    val old = g[r][c]; val m = g.size; val n = g[0].size
    val stack = ArrayDeque<Int>(); g[r][c] = color; stack.addLast(r * n + c)
    while (stack.isNotEmpty()) {
        val cell = stack.removeLast(); val x = cell / n; val y = cell % n
        for ((a, b) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
            if (a in 0 until m && b in 0 until n && g[a][b] == old) { g[a][b] = color; stack.addLast(a * n + b) }
        }
    }
    return g
}
