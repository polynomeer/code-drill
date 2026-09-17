// kind: OFF_BY_ONE
// 255 를 넘지 못한다고 하면서 255 자체도 거른다.
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean { if (piece.isEmpty() || piece.length > 3) return false; if (piece[0] == '0' && piece.length > 1) return false; return piece.toInt() < 255 }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) count += 1; return }
        for (length in 1..3) { if (start + length > n) break; if (valid(digits.substring(start, start + length))) go(start + length, parts + 1) }
    }
    go(0, 0)
    return count
}
