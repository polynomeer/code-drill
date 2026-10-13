// kind: MISSING_EDGE_CASE
// 재귀로 칠한다. 칸 하나 폭의 긴 섬은 깊이가 12 만이라 스택이 넘친다.
fun countSubIslands(grid1: Array<IntArray>, grid2: Array<IntArray>): Int {
    val m = grid1.size; val n = grid1[0].size
    val seen = Array(m) { BooleanArray(n) }
    var inside = true
    fun fill(x: Int, y: Int) {
        if (x !in 0 until m || y !in 0 until n || grid2[x][y] != 1 || seen[x][y]) return
        seen[x][y] = true
        if (grid1[x][y] == 0) inside = false
        fill(x + 1, y); fill(x - 1, y); fill(x, y + 1); fill(x, y - 1)
    }
    var count = 0
    for (i in 0 until m) for (j in 0 until n) if (grid2[i][j] == 1 && !seen[i][j]) { inside = true; fill(i, j); if (inside) count += 1 }
    return count
}
