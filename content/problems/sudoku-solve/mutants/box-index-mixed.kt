// kind: OFF_BY_ONE
// 상자 번호를 행/3 + 열/3 으로 센다. 다른 상자가 같은 번호를 나눠 갖는다.
fun solveSudoku(board: Array<IntArray>): Array<IntArray> {
    val rows = IntArray(9); val cols = IntArray(9); val boxes = IntArray(9)
    val blanks = ArrayList<Int>()
    for (r in 0 until 9) for (c in 0 until 9) {
        val v = board[r][c]
        if (v == 0) blanks.add(r * 9 + c)
        else { val bit = 1 shl v; rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[r / 3 + c / 3] = boxes[r / 3 + c / 3] or bit }
    }
    fun go(k: Int): Boolean {
        if (k == blanks.size) return true
        val cell = blanks[k]; val r = cell / 9; val c = cell % 9; val b = r / 3 + c / 3
        for (v in 1..9) {
            val bit = 1 shl v
            if (rows[r] and bit != 0 || cols[c] and bit != 0 || boxes[b] and bit != 0) continue
            rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit; board[r][c] = v
            if (go(k + 1)) return true
            rows[r] = rows[r] xor bit; cols[c] = cols[c] xor bit; boxes[b] = boxes[b] xor bit; board[r][c] = 0
        }
        return false
    }
    go(0)
    return board
}
