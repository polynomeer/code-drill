// kind: WRONG_ALGORITHM
// 빼는 규칙이 없다. IV 가 6 이 된다.
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    return text.sumOf { value(it) }
}
