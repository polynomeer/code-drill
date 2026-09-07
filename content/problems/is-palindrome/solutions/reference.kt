// 검증용 정답 (§6.1 solutions/). 양쪽에서 좁혀 온다.
//
// 볼 글자만 골라 새 문자열을 만들어도 되지만, 그러면 입력만큼의 메모리를 더 쓴다.
// 양쪽 포인터가 각자 다음 글자를 찾아 건너뛰면 추가 메모리 없이 끝난다.
fun isPalindrome(text: String): Int {
    var left = 0
    var right = text.length - 1

    while (left < right) {
        while (left < right && !text[left].isLetterOrDigit()) left += 1
        while (left < right && !text[right].isLetterOrDigit()) right -= 1
        if (left >= right) break

        Drill.pointer("left", left)
        Drill.pointer("right", right)
        Drill.compare(left, right)

        if (text[left].lowercaseChar() != text[right].lowercaseChar()) return 0
        left += 1
        right -= 1
    }
    return 1
}
