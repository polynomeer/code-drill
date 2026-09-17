// 검증용 정답 (§6.1 solutions/). 길이 1·2·3 을 시도하는 깊이 4 의 백트래킹.
fun validIpSplitsCount(digits: String): Int {
    val n = digits.length
    fun valid(piece: String): Boolean {
        if (piece.isEmpty() || piece.length > 3) return false
        if (piece[0] == '0' && piece.length > 1) return false
        return piece.toInt() <= 255
    }
    var count = 0
    fun go(start: Int, parts: Int) {
        if (parts == 4) { if (start == n) { count += 1; Drill.write(0, count) }; return }
        for (length in 1..3) {
            if (start + length > n) break
            Drill.compare(start, length)
            if (valid(digits.substring(start, start + length))) go(start + length, parts + 1)
        }
    }
    go(0, 0)
    return count
}
