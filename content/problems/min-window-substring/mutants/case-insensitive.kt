// kind: MISSING_EDGE_CASE
// 대소문자를 같게 본다.
fun minWindow(s: String, t: String): Int {
    val ls = s.lowercase(); val lt = t.lowercase()
    if (lt.isEmpty() || lt.length > ls.length) return 0
    val need = HashMap<Char, Int>()
    for (c in lt) need[c] = (need[c] ?: 0) + 1
    var missing = lt.length; var best = 0; var left = 0
    for (right in ls.indices) {
        val c = ls[right]; val n = need[c] ?: 0
        if (n > 0) missing -= 1
        need[c] = n - 1
        while (missing == 0) {
            val length = right - left + 1
            if (best == 0 || length < best) best = length
            val l = ls[left]; need[l] = (need[l] ?: 0) + 1; if (need[l]!! > 0) missing += 1; left += 1
        }
    }
    return best
}
