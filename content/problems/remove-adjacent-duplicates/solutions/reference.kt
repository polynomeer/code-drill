// 검증용 정답 (§6.1 solutions/). 왼쪽부터 쌓으며 맨 위와 같은 글자가 오면 함께 지운다 — 지운 뒤 새로 붙는 쌍도 이어서 지워진다.
fun removeAdjacentDuplicates(s: String): String {
    val stack = StringBuilder()
    for (c in s) {
        if (stack.isNotEmpty() && stack[stack.length - 1] == c) {
            stack.setLength(stack.length - 1)
            Drill.pop(c - 'a')
        } else {
            stack.append(c)
            Drill.push(c - 'a')
        }
    }
    return stack.toString()
}
