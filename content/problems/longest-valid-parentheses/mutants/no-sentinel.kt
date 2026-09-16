// kind: MISSING_EDGE_CASE
// 처음의 -1 경계가 없다. 문자열 시작에서 끝나는 구간을 못 잰다.
fun longestValidParentheses(s: String): Int {
    var best = 0
    val stack = ArrayDeque<Int>()
    for (i in s.indices) {
        if (s[i] == '(') stack.addLast(i)
        else {
            if (stack.isEmpty()) continue
            stack.removeLast()
            if (stack.isNotEmpty()) best = maxOf(best, i - stack.last())
        }
    }
    return best
}
