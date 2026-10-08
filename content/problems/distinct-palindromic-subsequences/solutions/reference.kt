// 검증용 정답 (§6.1 solutions/). 구간 DP — 양 끝이 같으면 가운데의 같은 글자가 몇 개인지로 나눈다.
fun distinctPalindromicSubsequences(s: String): Int {
    val mod = 1_000_000_007L
    val n = s.length
    val next = Array(n + 1) { IntArray(4) { n } }
    val prev = Array(n + 1) { IntArray(4) { -1 } }
    for (i in n - 1 downTo 0) { next[i] = next[i + 1].copyOf(); next[i][s[i] - 'a'] = i }
    for (i in 0 until n) { prev[i + 1] = prev[i].copyOf(); prev[i + 1][s[i] - 'a'] = i }
    val dp = Array(n + 1) { LongArray(n + 1) }
    for (length in 1..n) {
        for (i in 0..n - length) {
            val j = i + length
            val v: Long = if (s[i] != s[j - 1]) {
                dp[i + 1][j] + dp[i][j - 1] - dp[i + 1][j - 1]
            } else {
                val c = s[i] - 'a'
                val lo = next[i + 1][c]
                val hi = prev[j - 1][c]
                val inner = dp[i + 1][j - 1]
                when {
                    lo > hi -> 2 * inner + if (length > 1) 2 else 1
                    lo == hi -> 2 * inner + 1
                    else -> 2 * inner - dp[lo + 1][hi]
                }
            }
            dp[i][j] = ((v % mod) + mod) % mod
        }
        if (length == n) Drill.write(0, dp[0][n].toInt())
    }
    return dp[0][n].toInt()
}
