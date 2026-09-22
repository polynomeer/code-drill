// kind: MISSING_EDGE_CASE
// 숫자를 한 자리로만 읽는다. 12[a] 가 2 번이 된다.
fun decodeNestedString(text: String): String {
    val counts = ArrayDeque<Int>(); val strings = ArrayDeque<StringBuilder>()
    var current = StringBuilder(); var number = 0
    for (ch in text) {
        when {
            ch.isDigit() -> number = ch - '0'
            ch == '[' -> { counts.addLast(number); strings.addLast(current); current = StringBuilder(); number = 0 }
            ch == ']' -> { val repeat = counts.removeLast(); val previous = strings.removeLast(); val piece = current.toString(); repeat(repeat) { previous.append(piece) }; current = previous }
            else -> current.append(ch)
        }
    }
    return current.toString()
}
