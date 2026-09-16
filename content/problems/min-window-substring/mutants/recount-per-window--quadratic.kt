// kind: PERFORMANCE
// 시작점마다 개수를 처음부터 세며 늘린다. O(n²).
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty() || t.length > s.length) return 0
    val need = HashMap<Char, Int>()
    for (c in t) need[c] = (need[c] ?: 0) + 1
    var best = 0
    for (i in s.indices) {
        val have = HashMap<Char, Int>(); var covered = 0
        for (j in i until s.length) {
            Drill.compare(i, j)
            val c = s[j]; val h = (have[c] ?: 0) + 1; have[c] = h
            if (h <= (need[c] ?: 0)) covered += 1
            if (covered == t.length) { if (best == 0 || j - i + 1 < best) best = j - i + 1; break }
        }
    }
    return best
}
