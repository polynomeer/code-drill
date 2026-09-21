// kind: WRONG_BRANCH
// 이전 기호와 비교해 뺀다. 빼야 할 것은 앞의 작은 기호인데 뒤의 큰 기호를 뺀다.
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        total += if (i > 0 && value(text[i - 1]) < v) -v else v
    }
    return total
}
