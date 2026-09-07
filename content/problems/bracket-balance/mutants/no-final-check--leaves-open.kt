// kind: MISSING_EDGE_CASE
// 끝났을 때 스택이 비었는지 보지 않아, 열린 채로 끝나도 통과시킨다.
fun isBalanced(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()
    for (token in tokens) {
        if (token > 0) {
            stack.addLast(token)
        } else {
            if (stack.isEmpty()) return 0
            if (stack.removeLast() != -token) return 0
        }
    }
    return 1
}
