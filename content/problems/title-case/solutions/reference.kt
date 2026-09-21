// 검증용 정답 (§6.1 solutions/). 글자마다 "단어의 시작인가"를 들고 간다.
fun titleCase(text: String): String {
    val out = StringBuilder(text.length)
    var start = true
    for ((i, ch) in text.withIndex()) {
        Drill.compare(i, if (start) 1 else 0)
        when {
            ch == ' ' -> { out.append(ch); start = true }
            start -> { out.append(ch.uppercaseChar()); start = false }
            else -> out.append(ch.lowercaseChar())
        }
    }
    return out.toString()
}
