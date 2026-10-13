// kind: WRONG_BRANCH
// 처음 모음과 마지막 모음만 맞바꾼다. 안쪽 모음들도 차례로 바꿔야 한다.
fun reverseVowels(s: String): String {
    val chars = s.toCharArray()
    fun vowel(c: Char) = c in "aeiouAEIOU"
    val i = chars.indexOfFirst { vowel(it) }; val j = chars.indexOfLast { vowel(it) }
    if (i in 0 until j) { val t = chars[i]; chars[i] = chars[j]; chars[j] = t }
    return String(chars)
}
