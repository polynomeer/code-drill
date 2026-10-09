// kind: WRONG_BRANCH
// 소문자 갈래부터 내려가고 정렬하지 않는다. 대문자가 문자 코드에서 앞이라 순서가 거꾸로다.
fun letterCasePermutations(s: String): Array<String> {
    val out = ArrayList<String>()
    val chars = s.toCharArray()
    fun go(i: Int) {
        if (i == chars.size) { out.add(String(chars)); return }
        val c = s[i]
        if (c.isLetter()) { chars[i] = c.lowercaseChar(); go(i + 1); chars[i] = c.uppercaseChar(); go(i + 1); chars[i] = c } else go(i + 1)
    }
    go(0)
    return out.toTypedArray()
}
