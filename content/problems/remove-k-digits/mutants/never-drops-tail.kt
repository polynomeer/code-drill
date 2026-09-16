// kind: OFF_BY_ONE
// 끝까지 갔는데 k 가 남아도 뒤에서 지우지 않는다. 오름차순 입력에서 자리 수가 남는다.
fun removeKDigits(num: String, k: Int): String {
    val stack = StringBuilder()
    var left = k
    for (ch in num) {
        while (left > 0 && stack.isNotEmpty() && stack.last() > ch) { stack.setLength(stack.length - 1); left -= 1 }
        stack.append(ch)
    }
    val trimmed = stack.toString().trimStart('0')
    return if (trimmed.isEmpty()) "0" else trimmed
}
