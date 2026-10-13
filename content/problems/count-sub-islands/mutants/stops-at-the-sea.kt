// kind: WRONG_BRANCH
// grid1 의 바다 칸을 만나면 그 섬 칠하기를 멈춘다. 남은 칸이 나중에 다른 섬으로 세어진다.
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
            if (grid1[x][y] == 0) { inside = false; break }
            for ((a, b) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
                if (a in 0 until m && b in 0 until n && grid2[a][b] == 1 && !seen[a][b]) { seen[a][b] = true; stack.addLast(a * n + b) }
            }
        }
        if (inside) count += 1
    }
    return count
}
