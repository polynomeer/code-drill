// kind: MISSING_EDGE_CASE
// 빈 s 에 a*b* 가 맞는 첫 행을 채우지 않는다.
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') dp[i][j - 2] || ((p[j - 2] == '.' || p[j - 2] == s[i - 1]) && dp[i - 1][j])
        else (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
    }
    return if (dp[m][n]) 1 else 0
}
