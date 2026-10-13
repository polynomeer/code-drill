// kind: WRONG_ALGORITHM
// grid2 의 섬을 모두 센다. grid1 에서도 모두 땅인 섬만 센다.
fun countSubIslands(grid1: Array<IntArray>, grid2: Array<IntArray>): Int {
    val m = grid2.size; val n = grid2[0].size
    val seen = Array(m) { BooleanArray(n) }
    var count = 0
    for (i in 0 until m) for (j in 0 until n) {
        if (grid2[i][j] != 1 || seen[i][j]) continue
        count += 1
        val stack = ArrayDeque<Int>(); stack.addLast(i * n + j); seen[i][j] = true
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast(); val x = cell / n; val y = cell % n
            for ((a, b) in listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1)) {
                if (a in 0 until m && b in 0 until n && grid2[a][b] == 1 && !seen[a][b]) { seen[a][b] = true; stack.addLast(a * n + b) }
            }
        }
    }
    return count
}
