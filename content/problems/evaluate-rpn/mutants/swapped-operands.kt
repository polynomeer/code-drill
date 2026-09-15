// kind: WRONG_BRANCH
// 먼저 꺼낸 것을 앞 피연산자로 쓴다. 빼기와 나누기의 순서가 뒤집힌다.
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token >= 0) { stack.addLast(token); continue }
        val left = stack.removeLast()
        val right = stack.removeLast()
        stack.addLast(when (token) { -1 -> left + right; -2 -> left - right; -3 -> left * right; else -> left / right })
    }
    return stack.last()
}
