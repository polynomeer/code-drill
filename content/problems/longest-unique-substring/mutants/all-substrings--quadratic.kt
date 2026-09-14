// kind: PERFORMANCE
// 시작점마다 집합을 새로 만들어 늘려 간다. O(n²).
fun longestUnique(text: String): Int {
    var best = 0
    for (i in text.indices) {
        val seen = HashSet<Char>()
        var j = i
        while (j < text.length && seen.add(text[j])) { Drill.compare(i, j); j += 1 }
        best = maxOf(best, j - i)
    }
    return best
}
