// kind: OFF_BY_ONE
// 겹침을 짧은 쪽 길이보다 둘 적은 데서부터 찾는다. 한 글자만 빼고 다 겹치는 짝을 놓친다.
fun shortestSuperstring(words: Array<String>): Int {
    val unique = words.distinct()
    val kept = unique.filter { w -> unique.none { it != w && it.contains(w) } }
    val n = kept.size
    val overlap = Array(n) { IntArray(n) }
    for (i in 0 until n) for (j in 0 until n) {
        if (i == j) continue
        var k = minOf(kept[i].length, kept[j].length) - 2
        while (k > 0 && !kept[i].endsWith(kept[j].substring(0, k))) k -= 1
        overlap[i][j] = maxOf(k, 0)
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
