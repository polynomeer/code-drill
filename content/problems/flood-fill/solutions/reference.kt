// 검증용 정답 (§6.1 solutions/). 원래 색과 같은 이웃으로 스택을 들고 퍼진다. 색이 같으면 바꿀 것이 없다.
fun floodFill(grid: Array<IntArray>, r: Int, c: Int, color: Int): Array<IntArray> {
    val g = Array(grid.size) { grid[it].copyOf() }
    val old = g[r][c]
    if (old == color) return g
    val m = g.size; val n = g[0].size
    val stack = ArrayDeque<Int>()
    g[r][c] = color
    stack.addLast(r * n + c)
    while (stack.isNotEmpty()) {
        val cell = stack.removeLast(); val x = cell / n; val y = cell % n
        Drill.visit(cell, color)
        if (x + 1 < m && g[x + 1][y] == old) { g[x + 1][y] = color; stack.addLast((x + 1) * n + y) }
        if (x > 0 && g[x - 1][y] == old) { g[x - 1][y] = color; stack.addLast((x - 1) * n + y) }
        if (y + 1 < n && g[x][y + 1] == old) { g[x][y + 1] = color; stack.addLast(x * n + y + 1) }
        if (y > 0 && g[x][y - 1] == old) { g[x][y - 1] = color; stack.addLast(x * n + y - 1) }
    }
    return g
}
