// kind: WRONG_ALGORITHM
// 짝지어진 괄호의 수를 두 배 한다. 연속이 아니어도 센다.
fun longestValidParentheses(s: String): Int {
    var open = 0; var pairs = 0
    for (c in s) if (c == '(') open += 1 else if (open > 0) { open -= 1; pairs += 1 }
    return pairs * 2
}
