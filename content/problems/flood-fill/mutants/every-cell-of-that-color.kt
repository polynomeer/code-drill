// kind: WRONG_ALGORITHM
// 원래 색인 칸을 격자에서 모두 바꾼다. 시작 칸과 이어진 칸만 바꾼다.
fun floodFill(grid: Array<IntArray>, r: Int, c: Int, color: Int): Array<IntArray> {
    val old = grid[r][c]
    return Array(grid.size) { i -> IntArray(grid[i].size) { j -> if (grid[i][j] == old) color else grid[i][j] } }
}
