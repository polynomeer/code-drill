// kind: OFF_BY_ONE
// 문자열의 첫 글자만 대문자로 한다. 둘째 단어부터는 소문자다.
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    for ((i, ch) in text.withIndex()) out.append(if (i == 0) ch.uppercaseChar() else ch.lowercaseChar())
    return out.toString()
}
