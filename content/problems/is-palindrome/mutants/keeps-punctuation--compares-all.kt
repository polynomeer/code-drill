// kind: MISSING_EDGE_CASE
// 문장 부호와 공백까지 비교한다.
fun isPalindrome(text: String): Int {
    var left = 0
    var right = text.length - 1
    while (left < right) {
        if (text[left].lowercaseChar() != text[right].lowercaseChar()) return 0
        left += 1
        right -= 1
    }
    return 1
}
