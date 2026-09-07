// kind: WRONG_BRANCH
// 알파벳이 아닌 문자도 26으로 나눈 나머지 자리에 센다.
fun countLetters(text: String): IntArray {
    val counts = IntArray(26)
    for (ch in text) {
        val slot = (ch.lowercaseChar() - 'a')
        counts[((slot % 26) + 26) % 26] += 1
    }
    return counts
}
