// kind: MISSING_EDGE_CASE
// I 앞에서만 빼는 규칙을 적용한다. XL·XC·CD·CM 을 놓친다.
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        total += if (text[i] == 'I' && i + 1 < text.length && value(text[i + 1]) > v) -v else v
    }
    return total
}
