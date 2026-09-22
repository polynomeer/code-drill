// 검증용 정답 (§6.1 solutions/). 숫자 스택과 문자열 스택.
fun decodeNestedString(text: String): String {
    val counts = ArrayDeque<Int>()
    val strings = ArrayDeque<StringBuilder>()
    var current = StringBuilder()
    var number = 0
    for (ch in text) {
        when {
            ch.isDigit() -> number = number * 10 + (ch - '0')
            ch == '[' -> { Drill.compare(counts.size, number); counts.addLast(number); strings.addLast(current); current = StringBuilder(); number = 0 }
            ch == ']' -> {
                val repeat = counts.removeLast()
                val previous = strings.removeLast()
                val piece = current.toString()
                repeat(repeat) { previous.append(piece) }
                current = previous
                Drill.write(0, current.length)
            }
            else -> current.append(ch)
        }
    }
    return current.toString()
}
