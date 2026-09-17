// kind: MISSING_EDGE_CASE
// 앞의 0 을 허용한다. 010 이 조각이 된다.
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String) = piece.isNotEmpty() && piece.length <= 3 && piece.toInt() <= 255
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
