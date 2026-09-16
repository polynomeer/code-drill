// kind: WRONG_BRANCH
// * 를 1 번 이상으로 본다. 0 번 갈래가 없다.
fun regexMatch(s: String, p: String): Int {
    val m = s.length; val n = p.length
    val dp = Array(m + 1) { BooleanArray(n + 1) }
    dp[0][0] = true
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (p[j - 1] == '*') (p[j - 2] == '.' || p[j - 2] == s[i - 1]) && (dp[i - 1][j] || dp[i - 1][j - 2])
        else (p[j - 1] == '.' || p[j - 1] == s[i - 1]) && dp[i - 1][j - 1]
    }
    return if (dp[m][n]) 1 else 0
}
