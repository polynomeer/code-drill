// kind: OFF_BY_ONE
// 길이를 hi - lo 로 센다. 넓히기가 한 칸 넘어간 뒤라 하나 크다.
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 0
    for (center in 0 until n) {
        for (even in 0..1) {
            var lo = center; var hi = center + even
            while (lo >= 0 && hi < n && text[lo] == text[hi]) { lo -= 1; hi += 1 }
            best = maxOf(best, hi - lo)
        }
    }
    return best
}
