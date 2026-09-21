// kind: WRONG_BRANCH
// 공백을 지나도 단어의 시작으로 돌아가지 않는다.
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var start = true
    for (ch in text) {
        when {
            ch == ' ' -> out.append(ch)
            start -> { out.append(ch.uppercaseChar()); start = false }
            else -> out.append(ch.lowercaseChar())
        }
    }
    return out.toString()
}
