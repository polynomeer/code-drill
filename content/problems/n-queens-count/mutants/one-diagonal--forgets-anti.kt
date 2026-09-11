// kind: MISSING_EDGE_CASE
// 대각선을 하나만 본다. 반대 방향 대각선의 공격을 허용한다.
fun queens(n: Int): Int {
    val cols = BooleanArray(n); val diag1 = BooleanArray(2 * n)
    var count = 0
    fun go(r: Int) {
        if (r == n) { count += 1; return }
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n]) continue
            cols[c] = true; diag1[r - c + n] = true
            go(r + 1)
            cols[c] = false; diag1[r - c + n] = false
        }
    }
    go(0)
    return count
}
