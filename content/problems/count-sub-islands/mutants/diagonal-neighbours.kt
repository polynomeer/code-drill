// kind: WRONG_BRANCH
// 대각선 이웃도 같은 섬으로 본다. 섬은 상하좌우로만 이어진다.
fun countSubIslands(grid1: Array<IntArray>, grid2: Array<IntArray>): Int {
    val m = grid1.size; val n = grid1[0].size
    val seen = Array(m) { BooleanArray(n) }
    var count = 0
    for (i in 0 until m) for (j in 0 until n) {
        if (grid2[i][j] != 1 || seen[i][j]) continue
        var inside = true
        val stack = ArrayDeque<Int>(); stack.addLast(i * n + j); seen[i][j] = true
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast(); val x = cell / n; val y = cell % n
            if (grid1[x][y] == 0) inside = false
            for (dx in -1..1) for (dy in -1..1) {
                val a = x + dx; val b = y + dy
                if (a in 0 until m && b in 0 until n && grid2[a][b] == 1 && !seen[a][b]) { seen[a][b] = true; stack.addLast(a * n + b) }
            }
        }
        if (inside) count += 1
    }
    return count
}
