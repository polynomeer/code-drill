// kind: WRONG_BRANCH
// 되돌리기를 빠뜨린다. 한 가지가 남긴 표시가 형제 가지를 막는다.
fun queens(n: Int): Int {
    val cols = BooleanArray(n); val diag1 = BooleanArray(2 * n); val diag2 = BooleanArray(2 * n)
    var count = 0
    fun go(r: Int) {
        if (r == n) { count += 1; return }
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n] || diag2[r + c]) continue
            cols[c] = true; diag1[r - c + n] = true; diag2[r + c] = true
            go(r + 1)
        }
    }
    go(0)
    return count
}
