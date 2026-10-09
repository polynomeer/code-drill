// kind: WRONG_ALGORITHM
// 원래 문자열에서 영문자 하나씩만 바꾼 것들을 낸다. 여러 글자를 함께 바꾼 경우가 빠진다.
fun letterCasePermutations(s: String): Array<String> {
    val out = sortedSetOf(s)
    for (i in s.indices) if (s[i].isLetter()) {
        val c = s[i]
        val flipped = if (c.isUpperCase()) c.lowercaseChar() else c.uppercaseChar()
        out.add(s.substring(0, i) + flipped + s.substring(i + 1))
    }
    return out.toTypedArray()
}
