// kind: MISSING_EDGE_CASE
// 실제로 나온 글자만 담아 길이가 26이 아니다.
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)
    for (ch in text) {
        val lower = ch.lowercaseChar()
        if (lower < 'a' || lower > 'z') continue
        counts[lower - 'a'] += 1
    }
    return counts.filter { it > 0 }.toIntArray()
}
