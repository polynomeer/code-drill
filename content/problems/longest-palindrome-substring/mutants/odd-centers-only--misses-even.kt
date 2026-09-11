// kind: MISSING_EDGE_CASE
// 글자 위의 중심만 본다. 짝수 길이 회문을 놓친다.
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 0
    for (center in 0 until n) {
        var lo = center; var hi = center
        while (lo >= 0 && hi < n && text[lo] == text[hi]) { lo -= 1; hi += 1 }
        best = maxOf(best, hi - lo - 1)
    }
    return best
}
