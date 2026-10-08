// kind: PERFORMANCE
// 부분 수열을 모두 만들어 회문만 집합에 모은다. 부분 수열은 길이에 지수로 늘어난다.
fun distinctPalindromicSubsequences(s: String): Int {
    val seen = HashSet<String>()
    val picked = StringBuilder()
    fun go(i: Int) {
        if (i == s.length) {
            if (picked.isNotEmpty()) { val t = picked.toString(); if (t == t.reversed()) seen.add(t) }
            return
        }
        go(i + 1)
        picked.append(s[i])
        Drill.compare(i, picked.length)
        go(i + 1)
        picked.setLength(picked.length - 1)
    }
    go(0)
    return seen.size % 1_000_000_007
}
