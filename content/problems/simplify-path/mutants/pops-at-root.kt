// kind: MISSING_EDGE_CASE
// 루트에서 `..` 를 만나도 꺼낸다. 빈 스택에서 꺼내다 멈춘다.
fun simplifyPath(path: String): String {
    val stack = ArrayDeque<String>()
    for (part in path.split('/')) {
        when (part) {
            "", "." -> {}
            ".." -> stack.removeLast()
            else -> stack.addLast(part)
        }
    }
    return "/" + stack.joinToString("/")
}
