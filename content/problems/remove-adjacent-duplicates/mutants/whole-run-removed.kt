// kind: WRONG_BRANCH
// 같은 글자가 이어진 덩어리를 통째로 지운다. 둘씩 지우므로 셋이면 하나가 남는다.
fun removeAdjacentDuplicates(s: String): String {
    val stack = StringBuilder()
    var i = 0
    while (i < s.length) {
        var j = i
        while (j < s.length && s[j] == s[i]) j += 1
        if (j - i >= 2) { i = j; continue }
        if (stack.isNotEmpty() && stack[stack.length - 1] == s[i]) stack.setLength(stack.length - 1) else stack.append(s[i])
        i = j
    }
    return stack.toString()
}
