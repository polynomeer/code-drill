// 검증용 정답 (§6.1 solutions/). f(i) = f(i-1)[한 글자] + f(i-2)[두 글자].
fun decodeWays(digits: String): Int {
    if (digits.isEmpty()) return 0
    val mod = 1_000_000_007L
    var prev2 = 1L
    var prev1 = if (digits[0] != '0') 1L else 0L
    for (i in 1 until digits.length) {
        var cur = 0L
        if (digits[i] != '0') cur += prev1
        val pair = (digits[i - 1] - '0') * 10 + (digits[i] - '0')
        if (pair in 10..26) { cur += prev2; Drill.compare(i - 1, i) }
        prev2 = prev1
        prev1 = cur % mod
        Drill.visit(i, prev1.toInt())
    }
    return prev1.toInt()
}
