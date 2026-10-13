// kind: MISSING_EDGE_CASE
// 소문자 모음만 본다. 대문자 모음도 모음이다.
fun reverseVowels(s: String): String {
    val chars = s.toCharArray()
    fun vowel(c: Char) = c in "aeiou"
    var i = 0; var j = chars.size - 1
    while (i < j) {
        if (!vowel(chars[i])) i += 1 else if (!vowel(chars[j])) j -= 1 else { val t = chars[i]; chars[i] = chars[j]; chars[j] = t; i += 1; j -= 1 }
    }
    return String(chars)
}
