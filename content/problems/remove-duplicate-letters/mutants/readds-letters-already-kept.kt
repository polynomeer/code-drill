// kind: MISSING_EDGE_CASE
// 이미 스택에 있는 글자도 다시 넣는다. 같은 글자가 두 번 남는다.
fun removeDuplicateLetters(s: String): String {
    val last = IntArray(26)
    for (i in s.indices) last[s[i] - 'a'] = i
    val stack = StringBuilder()
    for (i in s.indices) {
        val c = s[i]
        if (stack.isNotEmpty() && stack.last() == c) continue
        while (stack.isNotEmpty() && stack.last() > c && last[stack.last() - 'a'] > i) stack.setLength(stack.length - 1)
        stack.append(c)
    }
    return stack.toString()
}
