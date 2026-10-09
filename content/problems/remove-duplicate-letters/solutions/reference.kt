// 검증용 정답 (§6.1 solutions/). 뒤에 또 나오는 더 큰 글자는 스택에서 빼고, 이미 있는 글자는 건너뛴다.
fun removeDuplicateLetters(s: String): String {
    val last = IntArray(26)
    for (i in s.indices) last[s[i] - 'a'] = i
    val inside = BooleanArray(26)
    val stack = StringBuilder()
    for (i in s.indices) {
        val c = s[i]
        if (inside[c - 'a']) continue
        while (stack.isNotEmpty() && stack.last() > c && last[stack.last() - 'a'] > i) {
            val top = stack.last()
            inside[top - 'a'] = false
            stack.setLength(stack.length - 1)
            Drill.pop(top - 'a')
        }
        stack.append(c)
        inside[c - 'a'] = true
        Drill.push(c - 'a')
    }
    return stack.toString()
}
