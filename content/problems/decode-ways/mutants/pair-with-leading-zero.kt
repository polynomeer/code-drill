// kind: WRONG_BRANCH
// 앞 자리가 0 인 두 글자 조각(01~09)도 허용한다.
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L; var prev1 = if (digits[0] != '0') 1L else 0L
    for (i in 1 until digits.length) {
        var cur = 0L
        if (digits[i] != '0') cur += prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 1..26) cur += prev2
        prev2 = prev1; prev1 = cur % mod
    }
    return prev1.toInt()
}
