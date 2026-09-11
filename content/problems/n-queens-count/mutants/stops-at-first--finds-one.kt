// kind: WRONG_BRANCH
// 해를 하나 찾으면 멈춘다. 있느냐를 묻는 문제로 읽었다.
fun queens(n: Int): Int {
    val cols = BooleanArray(n); val diag1 = BooleanArray(2 * n); val diag2 = BooleanArray(2 * n)
    fun go(r: Int): Boolean {
        if (r == n) return true
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n] || diag2[r + c]) continue
            cols[c] = true; diag1[r - c + n] = true; diag2[r + c] = true
            if (go(r + 1)) return true
            cols[c] = false; diag1[r - c + n] = false; diag2[r + c] = false
        }
        return false
    }
    return if (go(0)) 1 else 0
}
