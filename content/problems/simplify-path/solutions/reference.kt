// 검증용 정답 (§6.1 solutions/). 들어간 폴더를 스택에 쌓는다.
fun simplifyPath(path: String): String {
    val stack = ArrayDeque<String>()
    for (part in path.split('/')) {
        when (part) {
            "", "." -> {}
            ".." -> if (stack.isNotEmpty()) {
                stack.removeLast()
                Drill.pop(stack.size)
            }
            else -> {
                stack.addLast(part)
                Drill.push(stack.size)
            }
        }
    }
    return "/" + stack.joinToString("/")
}
