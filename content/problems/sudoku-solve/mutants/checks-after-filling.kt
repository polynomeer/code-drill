// kind: PERFORMANCE
// 규칙을 보지 않고 빈칸을 다 채운 뒤에야 판이 맞는지 확인한다. 9^빈칸.
fun solveSudoku(board: Array<IntArray>): Array<IntArray> {
    val blanks = ArrayList<Int>()
    for (r in 0 until 9) for (c in 0 until 9) if (board[r][c] == 0) blanks.add(r * 9 + c)
    fun valid(): Boolean {
        for (i in 0 until 9) {
            var row = 0; var col = 0; var box = 0
            for (j in 0 until 9) {
                row = row or (1 shl board[i][j])
                col = col or (1 shl board[j][i])
                box = box or (1 shl board[(i / 3) * 3 + j / 3][(i % 3) * 3 + j % 3])
            }
            if (row != 0b1111111110 || col != 0b1111111110 || box != 0b1111111110) return false
        }
        return true
    }
    fun go(k: Int): Boolean {
        if (k == blanks.size) return valid()
        val cell = blanks[k]
        for (v in 1..9) {
            board[cell / 9][cell % 9] = v
            Drill.compare(cell, v)
            if (go(k + 1)) return true
        }
        board[cell / 9][cell % 9] = 0
        return false
    }
    go(0)
    return board
}
