// kind: OFF_BY_ONE
// 가운데에 같은 글자가 하나 있을 때도 그 글자 하나를 새로 센다. 그 글자는 가운데에서 이미 세어졌다.
fun distinctPalindromicSubsequences(s: String): Int {
    val mod = 1_000_000_007L
    val n = s.length
    val next = Array(n + 1) { IntArray(4) { n } }
    val prev = Array(n + 1) { IntArray(4) { -1 } }
    for (i in n - 1 downTo 0) { next[i] = next[i + 1].copyOf(); next[i][s[i] - 'a'] = i }
    for (i in 0 until n) { prev[i + 1] = prev[i].copyOf(); prev[i + 1][s[i] - 'a'] = i }
    val dp = Array(n + 1) { LongArray(n + 1) }
    for (length in 1..n) for (i in 0..n - length) {
        val j = i + length
        val v: Long = if (s[i] != s[j - 1]) dp[i + 1][j] + dp[i][j - 1] - dp[i + 1][j - 1] else {
            val c = s[i] - 'a'; val lo = next[i + 1][c]; val hi = prev[j - 1][c]; val inner = dp[i + 1][j - 1]
            when { lo >= hi -> 2 * inner + if (length > 1) 2 else 1; else -> 2 * inner - dp[lo + 1][hi] }
        }
        dp[i][j] = ((v % mod) + mod) % mod
    }
    return dp[0][n].toInt()
}
