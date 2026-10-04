// kind: MISSING_EDGE_CASE
// 폴더가 하나도 남지 않으면 빈 문자열을 돌려준다. 루트는 `/` 다.
fun simplifyPath(path: String): String {
    val stack = ArrayDeque<String>()
    for (part in path.split('/')) {
        when (part) {
            "", "." -> {}
            ".." -> if (stack.isNotEmpty()) stack.removeLast()
            else -> stack.addLast(part)
        }
    }
    val out = StringBuilder()
    for (name in stack) out.append('/').append(name)
    return out.toString()
}
