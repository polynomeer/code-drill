// 검증용 정답 (§6.1 solutions/). 피연산자는 쌓고 연산자는 둘을 꺼낸다.
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token >= 0) { stack.addLast(token); Drill.push(token); continue }
        val right = stack.removeLast()
        val left = stack.removeLast()
        Drill.pop(right)
        Drill.pop(left)
        val value = when (token) {
            -1 -> left + right
            -2 -> left - right
            -3 -> left * right
            else -> left / right
        }
        stack.addLast(value)
        Drill.write(0, value)
    }
    return stack.last()
}
