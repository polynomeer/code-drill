// kind: MISSING_EDGE_CASE
// 위와 왼쪽만 보고 한 번 훑는다. 뒤쪽에 있는 1 을 보지 못한다.
fun nearestOne(grid: Array<IntArray>): Array<IntArray> {
    val rows = grid.size
    val cols = grid[0].size
    val big = 1_000_000
    val dist = Array(rows) { r -> IntArray(cols) { c -> if (grid[r][c] == 1) 0 else big } }
    for (r in 0 until rows) for (c in 0 until cols) {
        if (r > 0 && dist[r - 1][c] + 1 < dist[r][c]) dist[r][c] = dist[r - 1][c] + 1
        if (c > 0 && dist[r][c - 1] + 1 < dist[r][c]) dist[r][c] = dist[r][c - 1] + 1
    }
    return dist
}
