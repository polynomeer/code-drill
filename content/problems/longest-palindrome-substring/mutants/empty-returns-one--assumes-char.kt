// kind: MISSING_EDGE_CASE
// 빈 문자열에서 1 을 돌려준다. 글자가 하나는 있다고 가정했다.
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 1
    for (center in 0 until n) {
        for (even in 0..1) {
            var lo = center; var hi = center + even
            while (lo >= 0 && hi < n && text[lo] == text[hi]) { lo -= 1; hi += 1 }
            best = maxOf(best, hi - lo - 1)
        }
    }
    return best
}
