// kind: WRONG_BRANCH
// 대각선 이웃으로도 퍼진다. 이어진 것은 상하좌우뿐이다.
fun floodFill(grid: Array<IntArray>, r: Int, c: Int, color: Int): Array<IntArray> {
    val g = Array(grid.size) { grid[it].copyOf() }
    val old = g[r][c]; if (old == color) return g
    val m = g.size; val n = g[0].size
    val stack = ArrayDeque<Int>(); g[r][c] = color; stack.addLast(r * n + c)
    while (stack.isNotEmpty()) {
        val cell = stack.removeLast(); val x = cell / n; val y = cell % n
        for (dx in -1..1) for (dy in -1..1) {
            val a = x + dx; val b = y + dy
            if (a in 0 until m && b in 0 until n && g[a][b] == old) { g[a][b] = color; stack.addLast(a * n + b) }
        }
    }
    return g
}
