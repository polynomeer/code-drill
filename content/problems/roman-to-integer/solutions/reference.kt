// 검증용 정답 (§6.1 solutions/). 다음 기호가 더 크면 빼고, 아니면 더한다.
fun romanToInteger(text: String): Int {
    fun value(c: Char) = when (c) { 'I' -> 1; 'V' -> 5; 'X' -> 10; 'L' -> 50; 'C' -> 100; 'D' -> 500; else -> 1000 }
    var total = 0
    for (i in text.indices) {
        val v = value(text[i])
        if (i + 1 < text.length) Drill.compare(i, i + 1)
        total += if (i + 1 < text.length && value(text[i + 1]) > v) -v else v
        Drill.write(0, total)
    }
    return total
}
