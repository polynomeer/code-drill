// kind: OFF_BY_ONE
// k − 1 번 반복한다.
fun decodeNestedString(text: String): String {
    val counts = ArrayDeque<Int>(); val strings = ArrayDeque<StringBuilder>()
    var current = StringBuilder(); var number = 0
    for (ch in text) {
        when {
            ch.isDigit() -> number = number * 10 + (ch - '0')
            ch == '[' -> { counts.addLast(number); strings.addLast(current); current = StringBuilder(); number = 0 }
            ch == ']' -> { val repeat = counts.removeLast(); val previous = strings.removeLast(); val piece = current.toString(); repeat(repeat - 1) { previous.append(piece) }; current = previous }
            else -> current.append(ch)
        }
    }
    return current.toString()
}
