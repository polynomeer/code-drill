// kind: PERFORMANCE
// 글자가 맞는지 이웃을 다 뻗은 뒤에 본다. 어긋난 갈래를 끝까지 따라간다.
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        Drill.visit(r, c)
        if (i == word.length - 1) return board[r][c] == word[i]
        seen[r][c] = true
        val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
        var found = false
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) { found = true; break }
        }
        seen[r][c] = false
        return found && board[r][c] == word[i]
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
