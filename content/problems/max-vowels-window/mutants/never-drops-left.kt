// kind: WRONG_ALGORITHM
// 들어온 글자만 더하고 나간 글자를 빼지 않는다. 창이 아니라 앞부분 전체를 센다.
fun maxVowelsWindow(s: String, k: Int): Int {
    fun vowel(c: Char) = c in "aeiou"
    var count = 0
    for (i in 0 until k) if (vowel(s[i])) count += 1
    var best = count
    for (i in k until s.length) { if (vowel(s[i])) count += 1; if (count > best) best = count }
    return best
}
