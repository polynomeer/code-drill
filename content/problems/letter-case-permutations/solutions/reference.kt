// 검증용 정답 (§6.1 solutions/). 글자마다 갈래를 대문자부터 내려가는 백트래킹 — 결과가 그대로 사전순이다.
fun letterCasePermutations(s: String): Array<String> {
    val out = ArrayList<String>()
    val chars = s.toCharArray()
    fun go(i: Int) {
        if (i == chars.size) { out.add(String(chars)); return }
        val c = s[i]
        if (c.isLetter()) {
            chars[i] = c.uppercaseChar(); go(i + 1)
            chars[i] = c.lowercaseChar(); go(i + 1)
            chars[i] = c
        } else {
            go(i + 1)
        }
    }
    go(0)
    Drill.call(s)
    return out.toTypedArray()
}
