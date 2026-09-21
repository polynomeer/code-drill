// kind: MISSING_EDGE_CASE
// 첫 글자만 대문자로 하고 나머지는 그대로 둔다. 소문자로 내려야 한다.
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var start = true
    for (ch in text) {
        when {
            ch == ' ' -> { out.append(ch); start = true }
            start -> { out.append(ch.uppercaseChar()); start = false }
            else -> out.append(ch)
        }
    }
    return out.toString()
}
