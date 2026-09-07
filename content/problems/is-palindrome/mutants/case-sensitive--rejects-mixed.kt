// kind: WRONG_BRANCH
// 대소문자를 구분해 'Aa' 를 회문이 아니라고 본다.
fun isPalindrome(text: String): Int {
    var left = 0
    var right = text.length - 1
    while (left < right) {
        while (left < right && !text[left].isLetterOrDigit()) left += 1
        while (left < right && !text[right].isLetterOrDigit()) right -= 1
        if (left >= right) break
        if (text[left] != text[right]) return 0
        left += 1
        right -= 1
    }
    return 1
}
