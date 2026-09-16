// 검증용 정답 (§6.1 solutions/). dp[i][j] = s 의 앞 i 글자가 p 의 앞 j 글자에 맞는가.
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (j in 2..n) if (p[j - 1] == '*') dp[0][j] = dp[0][j - 2]
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') {
            dp[i][j - 2] || ((p[j - 2] == '.' || p[j - 2] == s[i - 1]) && dp[i - 1][j])
        } else {
            (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
        }
        if (dp[i][j]) Drill.visit(i, j)
    }
    if (dp[m][n]) Drill.match(m, n)
    return if (dp[m][n]) 1 else 0
}
