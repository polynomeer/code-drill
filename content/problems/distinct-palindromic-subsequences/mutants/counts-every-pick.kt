// kind: WRONG_ALGORITHM
// 뽑은 자리가 다르면 다른 것으로 센다. 같은 문자열은 한 번만 센다.
fun distinctPalindromicSubsequences(s: String): Int {
    val mod = 1_000_000_007L
    val n = s.length
    val dp = Array(n + 1) { LongArray(n + 1) }
    for (length in 1..n) for (i in 0..n - length) {
        val j = i + length
        val v = if (length == 1) 1L
            else if (s[i] == s[j - 1]) dp[i + 1][j] + dp[i][j - 1] + 1
            else dp[i + 1][j] + dp[i][j - 1] - dp[i + 1][j - 1]
        dp[i][j] = ((v % mod) + mod) % mod
    }
    return dp[0][n].toInt()
}
