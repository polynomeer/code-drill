// kind: WRONG_BRANCH
// 방문 표시를 하지 않아 같은 섬을 여러 번 센다.
fun countIslands(grid: Array<IntArray>): Int {
    var islands = 0
    for (row in grid) {
        for (cell in row) {
            if (cell == 0) continue
            // 이웃을 따라가지 않으므로 땅 칸 수가 곧 섬 수가 된다.
            islands += 1
        }
    }
    return islands
}
