// 검증용 정답 (§6.1 solutions/). 중심 2n-1 개에서 양쪽으로 넓힌다.
fun longestPalindrome(text: String): Int {
    val n = text.length
    var best = 0
    for (center in 0 until n) {
        for (even in 0..1) {
            var lo = center
            var hi = center + even
            while (lo >= 0 && hi < n && text[lo] == text[hi]) {
                Drill.compare(lo, hi)
                lo -= 1
                hi += 1
                Drill.pointer("lo", lo)
                Drill.pointer("hi", hi)
            }
            val length = hi - lo - 1
            if (length > best) { best = length; Drill.match(lo + 1, hi - 1) }
        }
    }
    return best
}
