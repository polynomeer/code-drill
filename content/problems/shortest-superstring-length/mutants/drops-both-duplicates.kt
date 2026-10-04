// kind: MISSING_EDGE_CASE
// 같은 낱말이 둘이면 서로가 서로 안에 들었다고 보고 둘 다 지운다. 같은 낱말은 하나를 남겨야 한다.
fun shortestSuperstring(words: Array<String>): Int {
    val kept = words.filterIndexed { i, w -> words.indices.none { j -> j != i && words[j].contains(w) } }
    val n = kept.size
    if (n == 0) return 0
    val overlap = Array(n) { IntArray(n) }
    for (i in 0 until n) for (j in 0 until n) {
        if (i == j) continue
        var k = minOf(kept[i].length, kept[j].length) - 1
        while (k > 0 && !kept[i].endsWith(kept[j].substring(0, k))) k -= 1
        overlap[i][j] = k
    }
    val full = (1 shl n) - 1
    val saved = Array(1 shl n) { IntArray(n) { -1 } }
    for (i in 0 until n) saved[1 shl i][i] = 0
    for (mask in 1..full) for (last in 0 until n) {
        if (saved[mask][last] < 0) continue
        for (next in 0 until n) {
            if (mask and (1 shl next) != 0) continue
            val to = mask or (1 shl next)
            saved[to][next] = maxOf(saved[to][next], saved[mask][last] + overlap[last][next])
        }
    }
    return kept.sumOf { it.length } - saved[full].max()
}
