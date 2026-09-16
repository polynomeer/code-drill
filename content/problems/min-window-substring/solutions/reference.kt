// 검증용 정답 (§6.1 solutions/). 글자별 모자란 수를 들고 창을 민다.
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty() || t.length > s.length) return 0
    val need = HashMap<Char, Int>()
    for (c in t) need[c] = (need[c] ?: 0) + 1
    var missing = t.length
    var best = 0
    var left = 0
    for (right in s.indices) {
        val c = s[right]
        val n = need[c] ?: 0
        if (n > 0) missing -= 1
        need[c] = n - 1
        Drill.pointer("right", right)
        while (missing == 0) {
            val length = right - left + 1
            if (best == 0 || length < best) { best = length; Drill.write(0, best) }
            val l = s[left]
            need[l] = (need[l] ?: 0) + 1
            if (need[l]!! > 0) missing += 1
            left += 1
            Drill.pointer("left", left)
        }
    }
    return best
}
