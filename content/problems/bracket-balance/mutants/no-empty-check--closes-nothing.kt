// kind: MISSING_EDGE_CASE
// 빈 스택에서 닫는 경우를 보지 않는다.
fun isBalanced(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token > 0) {
            stack.addLast(token)
        } else if (stack.isNotEmpty()) {
            if (stack.removeLast() != -token) return 0
        }
    }
    return if (stack.isEmpty()) 1 else 0
}
