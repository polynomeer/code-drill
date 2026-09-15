// kind: PERFORMANCE
// 앞에서부터 두 갈래로 재귀한다. 기억이 없어 지수적이다.
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    fun ways(i: Int): Long {
        if (i == digits.length) return 1L
        if (digits[i] == '0') return 0L
        Drill.visit(i, 0)
        var total = ways(i + 1)
        if (i + 1 < digits.length) {
            val pair = (digits[i] - '0') * 10 + (digits[i + 1] - '0')
            if (pair <= 26) total += ways(i + 2)
        }
        return total % mod
    }
    return ways(0).toInt()
}
