// kind: MISSING_EDGE_CASE
// 재귀로 퍼진다. 칸 하나 폭의 긴 길은 깊이가 12 만이라 스택이 넘친다.
fun floodFill(grid: Array<IntArray>, r: Int, c: Int, color: Int): Array<IntArray> {
    val g = Array(grid.size) { grid[it].copyOf() }
    val old = g[r][c]; if (old == color) return g
    fun fill(x: Int, y: Int) {
        if (x !in g.indices || y !in g[0].indices || g[x][y] != old) return
        g[x][y] = color
        fill(x + 1, y); fill(x - 1, y); fill(x, y + 1); fill(x, y - 1)
    }
    fill(r, c)
    return g
}
