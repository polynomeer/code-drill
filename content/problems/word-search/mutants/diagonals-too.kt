// kind: WRONG_ALGORITHM
// 대각선 이웃으로도 뻗는다.
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        if (i == word.length - 1) return true
        seen[r][c] = true
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr; val nc = c + dc
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) { seen[r][c] = false; return true }
        }
        seen[r][c] = false
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
