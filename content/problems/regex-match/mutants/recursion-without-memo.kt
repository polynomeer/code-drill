// kind: PERFORMANCE
// 재귀로 두 갈래를 뻗는다. a*a*…b 에서 지수적이다.
fun regexMatch(s: String, p: String): Int {
    fun go(i: Int, j: Int): Boolean {
        if (j == p.length) return i == s.length
        Drill.visit(i, j)
        val first = i < s.length && (p[j] == '.' || p[j] == s[i])
        return if (j + 1 < p.length && p[j + 1] == '*') go(i, j + 2) || (first && go(i + 1, j))
        else first && go(i + 1, j + 1)
    }
    return if (go(0, 0)) 1 else 0
}
