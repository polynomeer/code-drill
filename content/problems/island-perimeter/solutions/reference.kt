// 검증용 정답 (§6.1 solutions/). 칸마다 네 변에서 시작해 이웃과 맞닿은 변을 뺀다.
//
// 덩어리를 찾아 다닐 필요가 없다. 둘레는 칸마다 독립적으로 정해지므로 한 번 훑으면
// 끝나고, 그래서 섬이 몇 개든 같은 코드가 답한다.
fun perimeter(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    var total = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (grid[r][c] != 1) continue
            Drill.visit(r * cols + c, grid[r][c])

            total += 4
            if (r > 0 && grid[r - 1][c] == 1) total -= 1
            if (r + 1 < rows && grid[r + 1][c] == 1) total -= 1
            if (c > 0 && grid[r][c - 1] == 1) total -= 1
            if (c + 1 < cols && grid[r][c + 1] == 1) total -= 1
        }
    }
    return total
}
