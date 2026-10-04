// kind: PERFORMANCE
// 이어 붙일 순서를 모두 해 본다. 낱말이 열여섯이면 순서가 20조 가지다.
fun shortestSuperstring(words: Array<String>): Int {
    val unique = words.distinct()
    val kept = unique.filter { w -> unique.none { it != w && it.contains(w) } }
    val n = kept.size
    val overlap = Array(n) { IntArray(n) }
    for (i in 0 until n) for (j in 0 until n) {
        if (i == j) continue
        var k = minOf(kept[i].length, kept[j].length) - 1
        while (k > 0 && !kept[i].endsWith(kept[j].substring(0, k))) k -= 1
        overlap[i][j] = k
    }
    var best = 0
    val used = BooleanArray(n)
    fun go(last: Int, count: Int, saved: Int) {
        if (count == n) { best = maxOf(best, saved); return }
        for (next in 0 until n) {
            if (used[next]) continue
            used[next] = true
            Drill.compare(last, next)
            go(next, count + 1, saved + overlap[last][next])
            used[next] = false
        }
    }
    for (first in 0 until n) { used[first] = true; go(first, 1, 0); used[first] = false }
    return kept.sumOf { it.length } - best
}
