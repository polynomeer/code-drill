// kind: WRONG_BRANCH
// 괄호를 열 때 지금까지의 문자열을 버린다. ab2[cd] 에서 ab 가 사라진다.
fun decodeNestedString(text: String): String {
    val counts = ArrayDeque<Int>(); val strings = ArrayDeque<StringBuilder>()
    var current = StringBuilder(); var number = 0
    for (ch in text) {
        when {
            ch.isDigit() -> number = number * 10 + (ch - '0')
            ch == '[' -> { counts.addLast(number); strings.addLast(StringBuilder()); current = StringBuilder(); number = 0 }
            ch == ']' -> { val repeat = counts.removeLast(); val previous = strings.removeLast(); val piece = current.toString(); repeat(repeat) { previous.append(piece) }; current = previous }
            else -> current.append(ch)
        }
    }
    return current.toString()
}
