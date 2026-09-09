// kind: OFF_BY_ONE
// 행 경계를 확인하지 않아 오른쪽 끝과 다음 행 왼쪽 끝을 이어버린다.
//
// 격자를 안에서 다시 평탄화해 놓고 좌우 이웃을 ±1 로만 구한다. 편 배열에서는 한 행의
// 마지막 칸과 다음 행의 첫 칸이 나란히 있으므로, 화면에서 떨어져 있는 둘이 이어진다.
fun countIslands(grid: Array<IntArray>): Int {
    val rows = grid.size
    val cols = if (rows == 0) 0 else grid[0].size
    val flat = IntArray(rows * cols) { grid[it / cols][it % cols] }
    val seen = BooleanArray(flat.size)
    var islands = 0

    for (start in flat.indices) {
        if (flat[start] == 0 || seen[start]) continue
        islands += 1
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        seen[start] = true
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            for (next in listOf(current - cols, current + cols, current - 1, current + 1)) {
                if (next < 0 || next >= flat.size) continue
                if (flat[next] == 0 || seen[next]) continue
                seen[next] = true
                stack.addLast(next)
            }
        }
    }
    return islands
}
