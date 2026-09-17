// kind: WRONG_BRANCH
// 0 으로 시작하는 조각을 전부 거른다. 조각 0 하나도 안 된다고 한다.
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean { if (piece.isEmpty() || piece.length > 3) return false; if (piece[0] == '0') return false; return piece.toInt() <= 255 }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
