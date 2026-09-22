// kind: WRONG_BRANCH
// 괄호를 연 뒤 숫자를 0 으로 되돌리지 않는다. 다음 숫자가 앞 숫자에 이어 붙는다.
fun decodeNestedString(text: String): String {
    val counts = ArrayDeque<Int>(); val strings = ArrayDeque<StringBuilder>()
    var current = StringBuilder(); var number = 0
    for (ch in text) {
        when {
            ch.isDigit() -> number = number * 10 + (ch - '0')
            ch == '[' -> { counts.addLast(number); strings.addLast(current); current = StringBuilder() }
            ch == ']' -> { val repeat = counts.removeLast(); val previous = strings.removeLast(); val piece = current.toString(); repeat(repeat) { previous.append(piece) }; current = previous }
            else -> current.append(ch)
        }
    }
    return current.toString()
}
