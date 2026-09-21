// kind: OFF_BY_ONE
// 다음 기호가 같아도 뺀다. III 가 1 이 된다.
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        total += if (i + 1 < text.length && value(text[i + 1]) >= v) -v else v
    }
    return total
}
