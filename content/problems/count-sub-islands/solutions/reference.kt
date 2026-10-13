// 검증용 정답 (§6.1 solutions/). grid2 의 섬을 스택으로 끝까지 칠하며, grid1 에서 바다인 칸이 있었는지 표시한다.
fun countSubIslands(grid1: Array<IntArray>, grid2: Array<IntArray>): Int {
    val m = grid1.size; val n = grid1[0].size
    val seen = Array(m) { BooleanArray(n) }
    val stack = IntArray(m * n)
    var count = 0
    for (i in 0 until m) for (j in 0 until n) {
        if (grid2[i][j] != 1 || seen[i][j]) continue
        var inside = true
        var size = 0
        stack[size++] = i * n + j; seen[i][j] = true
        while (size > 0) {
            val cell = stack[--size]; val x = cell / n; val y = cell % n
            Drill.visit(cell, 1)
            if (grid1[x][y] == 0) inside = false
            if (x + 1 < m && grid2[x + 1][y] == 1 && !seen[x + 1][y]) { seen[x + 1][y] = true; stack[size++] = (x + 1) * n + y }
            if (x > 0 && grid2[x - 1][y] == 1 && !seen[x - 1][y]) { seen[x - 1][y] = true; stack[size++] = (x - 1) * n + y }
            if (y + 1 < n && grid2[x][y + 1] == 1 && !seen[x][y + 1]) { seen[x][y + 1] = true; stack[size++] = x * n + y + 1 }
            if (y > 0 && grid2[x][y - 1] == 1 && !seen[x][y - 1]) { seen[x][y - 1] = true; stack[size++] = x * n + y - 1 }
        }
        if (inside) count += 1
    }
    return count
}
