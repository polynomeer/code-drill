// 검증용 정답 (§6.1 solutions/). 단조 증가 스택 — 앞 자리가 크면 지운다.
fun removeKDigits(num: String, k: Int): String {
    val stack = CharArray(num.length)
    var top = 0
    var left = k
    for (ch in num) {
        while (left > 0 && top > 0 && stack[top - 1] > ch) {
            Drill.compare(stack[top - 1] - '0', ch - '0')
            Drill.pop(stack[top - 1] - '0')
            top -= 1
            left -= 1
        }
        stack[top] = ch
        Drill.push(ch - '0')
        top += 1
    }
    top -= left
    var start = 0
    while (start < top && stack[start] == '0') start += 1
    return if (start == top) "0" else String(stack, start, top - start)
}
