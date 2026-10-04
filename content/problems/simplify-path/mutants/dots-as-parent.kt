// kind: WRONG_BRANCH
// 점으로만 된 조각을 모두 위 폴더로 본다. `...` 도 이름이다.
fun simplifyPath(path: String): String {
    val stack = ArrayDeque<String>()
    for (part in path.split('/')) {
        if (part.isEmpty() || part == ".") continue
        if (part.all { it == '.' }) { if (stack.isNotEmpty()) stack.removeLast() } else stack.addLast(part)
    }
    return "/" + stack.joinToString("/")
}
