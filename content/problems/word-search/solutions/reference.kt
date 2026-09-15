// 검증용 정답 (§6.1 solutions/). 이웃으로 뻗고, 실패하면 표시를 되돌린다.
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size
    val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        Drill.visit(r, c)
        if (i == word.length - 1) { Drill.match(r, c); return true }
        seen[r][c] = true
        val dr = intArrayOf(1, -1, 0, 0)
        val dc = intArrayOf(0, 0, 1, -1)
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) { seen[r][c] = false; return true }
        }
        seen[r][c] = false
        return false
    }
    if (word.length > rows * cols) return 0
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
