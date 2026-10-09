// kind: OFF_BY_ONE
// 원래 문자열을 결과에서 뺀다. 원래 문자열도 바꾸기의 한 경우다.
fun letterCasePermutations(s: String): Array<String> {
    val out = ArrayList<String>()
    val chars = s.toCharArray()
    fun go(i: Int) {
        if (i == chars.size) { val t = String(chars); if (t != s) out.add(t); return }
        val c = s[i]
        if (c.isLetter()) { chars[i] = c.uppercaseChar(); go(i + 1); chars[i] = c.lowercaseChar(); go(i + 1); chars[i] = c } else go(i + 1)
    }
    go(0)
    return out.toTypedArray()
}
