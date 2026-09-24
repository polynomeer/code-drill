// kind: WRONG_BRANCH
// 행과 열만 보고 3 x 3 상자는 보지 않는다. 상자 안에서 숫자가 겹친다.
fun solveSudoku(board: Array<IntArray>): Array<IntArray> {
    val rows = IntArray(9); val cols = IntArray(9)
    val blanks = ArrayList<Int>()
    for (r in 0 until 9) for (c in 0 until 9) {
        val v = board[r][c]
        if (v == 0) blanks.add(r * 9 + c)
        else { val bit = 1 shl v; rows[r] = rows[r] or bit; cols[c] = cols[c] or bit }
    }
    fun go(k: Int): Boolean {
        if (k == blanks.size) return true
        val cell = blanks[k]; val r = cell / 9; val c = cell % 9
        for (v in 1..9) {
            val bit = 1 shl v
            if (rows[r] and bit != 0 || cols[c] and bit != 0) continue
            rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; board[r][c] = v
            if (go(k + 1)) return true
            rows[r] = rows[r] xor bit; cols[c] = cols[c] xor bit; board[r][c] = 0
        }
        return false
    }
    go(0)
    return board
}
