// kind: WRONG_ALGORITHM
// 패턴이 앞부분에 맞으면 맞다고 본다. 전체여야 한다.
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (j in 2..n) if (p[j - 1] == '*') dp[0][j] = dp[0][j - 2]
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') dp[i][j - 2] || ((p[j - 2] == '.' || p[j - 2] == s[i - 1]) && dp[i - 1][j])
        else (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
    }
    return if ((0..m).any { dp[it][n] }) 1 else 0
}
