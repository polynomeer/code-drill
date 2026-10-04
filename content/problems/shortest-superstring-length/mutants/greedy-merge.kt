// kind: WRONG_ALGORITHM
// 가장 많이 겹치는 짝부터 이어 붙인다. 지금 가장 많이 겹치는 선택이 끝까지 가장 짧지는 않다.
fun shortestSuperstring(words: Array<String>): Int {
    fun overlap(a: String, b: String): Int {
        var k = minOf(a.length, b.length) - 1
        while (k > 0 && !a.endsWith(b.substring(0, k))) k -= 1
        return k
    }
    fun prune(list: List<String>): MutableList<String> {
        val unique = list.distinct()
        return unique.filter { w -> unique.none { it != w && it.contains(w) } }.toMutableList()
    }
    var pool = prune(words.toList())
    while (pool.size > 1) {
        var best = -1; var bi = 0; var bj = 1
        for (i in pool.indices) for (j in pool.indices) {
            if (i == j) continue
            val o = overlap(pool[i], pool[j])
            if (o > best) { best = o; bi = i; bj = j }
        }
        val merged = pool[bi] + pool[bj].substring(best)
        val rest = pool.filterIndexed { k, _ -> k != bi && k != bj }
        pool = prune(rest + merged)
    }
    return pool[0].length
}
