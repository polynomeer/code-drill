// kind: MISSING_EDGE_CASE
// 소문자만 센다.
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)
    for (ch in text) {
        if (ch < 'a' || ch > 'z') continue
        counts[ch - 'a'] += 1
    }
    return counts
}
