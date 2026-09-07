// 검증용 정답 (§6.1 solutions/). 스택.
//
// 세 가지를 모두 본다. 닫을 때 (1) 스택이 비어 있지 않고 (2) 짝이 맞아야 하며,
// (3) 끝났을 때 스택이 비어 있어야 한다. 하나라도 빼면 통과하는 잘못된 입력이 생긴다.
fun isBalanced(tokens: IntArray): Int {
    val stack = ArrayDeque<Int>()

    for (token in tokens) {
        if (token > 0) {
            stack.addLast(token)
            Drill.push(token)
        } else {
            if (stack.isEmpty()) return 0
            val open = stack.removeLast()
            Drill.pop(open)
            if (open != -token) return 0
        }
    }
    return if (stack.isEmpty()) 1 else 0
}
