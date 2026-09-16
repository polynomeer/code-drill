// kind: WRONG_ALGORITHM
// 글자가 있는지만 본다. 개수를 잊는다.
fun minWindow(s: String, t: String): Int {
    if (t.isEmpty()) return 0
    val need = t.toSet()
    var best = 0
    for (i in s.indices) {
        val seen = HashSet<Char>()
        for (j in i until s.length) { if (s[j] in need) seen.add(s[j]); if (seen.size == need.size) { if (best == 0 || j - i + 1 < best) best = j - i + 1; break } }
    }
    return best
}
