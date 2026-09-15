// kind: MISSING_EDGE_CASE
// 0 을 한 글자로도 센다. 0 으로 시작하는 조각은 없다.
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L; var prev1 = 1L
    for (i in 1 until digits.length) {
        var cur = prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 10..26) cur += prev2
        prev2 = prev1; prev1 = cur % mod
    }
    return prev1.toInt()
}
