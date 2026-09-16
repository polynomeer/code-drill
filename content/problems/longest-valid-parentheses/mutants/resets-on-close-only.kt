// kind: WRONG_BRANCH
// 짝이 맞을 때마다 2 를 더하고 어긋나면 0 으로 되돌린다 — 안긴 괄호를 못 센다.
fun longestValidParentheses(s: String): Int {
    var best = 0; var current = 0; var open = 0
    for (c in s) {
        if (c == '(') open += 1
        else if (open > 0) { open -= 1; current += 2; best = maxOf(best, current) }
        else { current = 0; open = 0 }
    }
    return best
}
