// kind: PERFORMANCE
// 시작점마다 균형을 세며 끝까지 간다. O(n²).
fun longestValidParentheses(s: String): Int {
    var best = 0
    for (i in s.indices) {
        var balance = 0
        for (j in i until s.length) {
            Drill.compare(i, j)
            if (s[j] == '(') balance += 1 else balance -= 1
            if (balance < 0) break
            if (balance == 0) best = maxOf(best, j - i + 1)
        }
    }
    return best
}
