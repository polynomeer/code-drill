// kind: OFF_BY_ONE
// 행 경계를 확인하지 않아 오른쪽 끝과 다음 행 왼쪽 끝을 이어버린다.
fun countIslands(grid: IntArray, width: Int): Int {
    val seen = BooleanArray(grid.size)
    var islands = 0
    for (start in grid.indices) {
        if (grid[start] == 0 || seen[start]) continue
        islands += 1
        val stack = ArrayDeque<Int>()
        stack.addLast(start)
        seen[start] = true
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            for (next in listOf(current - width, current + width, current - 1, current + 1)) {
                if (next < 0 || next >= grid.size) continue
                if (grid[next] == 0 || seen[next]) continue
                seen[next] = true
                stack.addLast(next)
            }
        }
    }
    return islands
}
