// kind: MISSING_EDGE_CASE
// 숫자도 두 갈래로 내려간다. 숫자의 대소문자는 같아 같은 문자열이 두 번 들어간다.
fun letterCasePermutations(s: String): Array<String> {
    val out = ArrayList<String>()
    val chars = s.toCharArray()
    fun go(i: Int) {
        if (i == chars.size) { out.add(String(chars)); return }
        val c = s[i]
        chars[i] = c.uppercaseChar(); go(i + 1)
        chars[i] = c.lowercaseChar(); go(i + 1)
        chars[i] = c
    }
    go(0)
    return out.toTypedArray()
}
