// kind: MISSING_EDGE_CASE
// 앞에 남은 0 을 지우지 않는다.
fun removeKDigits(num: String, k: Int): String {
    val stack = StringBuilder()
    var left = k
    for (ch in num) {
        while (left > 0 && stack.isNotEmpty() && stack.last() > ch) { stack.setLength(stack.length - 1); left -= 1 }
        stack.append(ch)
    }
    stack.setLength(stack.length - left)
    return if (stack.isEmpty()) "0" else stack.toString()
}
