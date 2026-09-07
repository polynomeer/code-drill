// kind: WRONG_BRANCH
// 숫자를 볼 글자로 치지 않는다.
fun isPalindrome(text: String): Int {
    val kept = text.filter { it.isLetter() }.lowercase()
    return if (kept == kept.reversed()) 1 else 0
}
