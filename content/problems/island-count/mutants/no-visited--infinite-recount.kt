// kind: WRONG_BRANCH
// 방문 표시를 하지 않아 같은 섬을 여러 번 센다.
fun countIslands(grid: IntArray, width: Int): Int {
    val height = grid.size / width
    var islands = 0
    for (start in grid.indices) {
        if (grid[start] == 0) continue
        islands += 1
        // 이웃을 따라가지 않으므로 땅 칸 수가 곧 섬 수가 된다.
        if (height < 0) return -1
    }
    return islands
}
