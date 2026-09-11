// 검증용 정답 (§6.1 solutions/). 행마다 하나, 열·두 대각선 표시, 되돌리기.
fun queens(n: Int): Int {
    val cols = BooleanArray(n)
    val diag1 = BooleanArray(2 * n)
    val diag2 = BooleanArray(2 * n)
    var count = 0
    fun go(r: Int) {
        Drill.call("row=$r")
        if (r == n) { count += 1; Drill.match(r, count); return }
        for (c in 0 until n) {
            if (cols[c] || diag1[r - c + n] || diag2[r + c]) continue
            cols[c] = true; diag1[r - c + n] = true; diag2[r + c] = true
            Drill.push(c)
            go(r + 1)
            Drill.pop(c)
            cols[c] = false; diag1[r - c + n] = false; diag2[r + c] = false
        }
    }
    go(0)
    return count
}
