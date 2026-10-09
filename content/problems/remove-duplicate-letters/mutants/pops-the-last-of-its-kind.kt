// kind: WRONG_BRANCH
// 맨 위 글자가 뒤에 또 나오는지 보지 않고 뺀다. 마지막으로 나온 글자를 빼면 그 글자가 답에서 사라진다.
fun removeDuplicateLetters(s: String): String {
    val inside = BooleanArray(26)
    val stack = StringBuilder()
    for (c in s) {
        if (inside[c - 'a']) continue
        while (stack.isNotEmpty() && stack.last() > c) { inside[stack.last() - 'a'] = false; stack.setLength(stack.length - 1) }
        stack.append(c); inside[c - 'a'] = true
    }
    return stack.toString()
}
