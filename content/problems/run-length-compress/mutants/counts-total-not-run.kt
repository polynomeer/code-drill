// kind: WRONG_ALGORITHM
// 연속한 구간이 아니라 그 글자의 전체 개수를 붙인다.
fun runLengthCompress(text: String): String {
    val counts = IntArray(26)
    for (ch in text) counts[ch - 'a'] += 1
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        var end = i
        while (end < text.length && text[end] == ch) end += 1
        out.append(ch)
        if (counts[ch - 'a'] > 1) out.append(counts[ch - 'a'])
        i = end
    }
    return out.toString()
}
