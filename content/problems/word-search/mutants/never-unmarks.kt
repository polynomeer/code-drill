// kind: WRONG_BRANCH
// 실패한 갈래의 표시를 되돌리지 않는다. 다른 갈래가 그 칸을 못 쓴다.
fun wordSearch(board: Array<String>, word: String): Int {
    val rows = board.size; val cols = board[0].length
    val seen = Array(rows) { BooleanArray(cols) }
    fun walk(r: Int, c: Int, i: Int): Boolean {
        if (board[r][c] != word[i]) return false
        if (i == word.length - 1) return true
        seen[r][c] = true
        val dr = intArrayOf(1, -1, 0, 0); val dc = intArrayOf(0, 0, 1, -1)
        for (d in 0 until 4) {
            val nr = r + dr[d]; val nc = c + dc[d]
            if (nr in 0 until rows && nc in 0 until cols && !seen[nr][nc] && walk(nr, nc, i + 1)) return true
        }
        return false
    }
    for (r in 0 until rows) for (c in 0 until cols) if (walk(r, c, 0)) return 1
    return 0
}
