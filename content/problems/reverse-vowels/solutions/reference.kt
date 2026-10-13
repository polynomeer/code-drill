// 검증용 정답 (§6.1 solutions/). 양 끝의 포인터가 모음에서 멈추면 맞바꾸고 안으로 온다.
fun reverseVowels(s: String): String {
    val chars = s.toCharArray()
    fun vowel(c: Char) = c in "aeiouAEIOU"
    var i = 0
    var j = chars.size - 1
    while (i < j) {
        when {
            !vowel(chars[i]) -> i += 1
            !vowel(chars[j]) -> j -= 1
            else -> {
                val t = chars[i]; chars[i] = chars[j]; chars[j] = t
                Drill.swap(i, j)
                i += 1; j -= 1
            }
        }
    }
    return String(chars)
}
