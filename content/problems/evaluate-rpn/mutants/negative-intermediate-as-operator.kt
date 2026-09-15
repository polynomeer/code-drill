// kind: WRONG_ALGORITHM
// 중간 값이 음수면 그것을 연산자로 착각한다. 인코딩은 입력에만 있다.
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    fun apply(op: Int) {
        val right = stack.removeLast(); val left = stack.removeLast()
        val value = when (op) { -1 -> left + right; -2 -> left - right; -3 -> left * right; else -> left / right }
        if (value < 0 && value >= -4 && stack.size >= 2) apply(value) else stack.addLast(value)
    }
    for (token in tokens) if (token >= 0) stack.addLast(token) else apply(token)
    return stack.last()
}
