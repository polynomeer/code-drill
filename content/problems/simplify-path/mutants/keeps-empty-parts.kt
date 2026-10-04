// kind: WRONG_BRANCH
// 빗금 사이의 빈 조각을 건너뛰지 않는다. 겹친 빗금이 빈 이름의 폴더가 된다.
fun simplifyPath(path: String): String {
    val stack = ArrayDeque<String>()
    for (part in path.substring(1).split('/')) {
        when (part) {
            "." -> {}
            ".." -> if (stack.isNotEmpty()) stack.removeLast()
            else -> stack.addLast(part)
        }
    }
    return "/" + stack.joinToString("/")
}
