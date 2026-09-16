// 검증용 정답 (§6.1 solutions/). 스택에 인덱스를 둔다. -1 이 첫 경계다.
fun longestValidParentheses(s: String): Int {
    var best = 0
    val stack = ArrayDeque<Int>()
    stack.addLast(-1)
    for (i in s.indices) {
        if (s[i] == '(') { stack.addLast(i); Drill.push(i) }
        else {
            stack.removeLast()
            Drill.pop(i)
            if (stack.isEmpty()) stack.addLast(i)
            else { val length = i - stack.last(); if (length > best) { best = length; Drill.write(0, best) } }
        }
    }
    return best
}
