// kind: MISSING_EDGE_CASE
// 나눗셈을 내림으로 한다. 음수에서 0 을 향해 자르지 않는다.
fun evaluateRpn(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token >= 0) { stack.addLast(token); continue }
        val right = stack.removeLast()
        val left = stack.removeLast()
        stack.addLast(when (token) { -1 -> left + right; -2 -> left - right; -3 -> left * right; else -> Math.floorDiv(left, right) })
    }
    return stack.last()
}
