// kind: PERFORMANCE
// 모든 단어 쌍을 견줘 간선을 만든다. O(n²·L).
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val all = (listOf(begin) + words).distinct()
    val idx = all.withIndex().associate { it.value to it.index }
    val target = idx[end] ?: return 0
    fun adjacent(a: String, b: String): Boolean { var d = 0; for (i in a.indices) { if (a[i] != b[i]) d += 1; if (d > 1) return false }; return d == 1 }
    val adj = Array(all.size) { ArrayList<Int>() }
    for (i in all.indices) for (j in i + 1 until all.size) { Drill.compare(i, j); if (adjacent(all[i], all[j])) { adj[i].add(j); adj[j].add(i) } }
    val dist = IntArray(all.size) { 0 }
    dist[0] = 1
    val queue = ArrayDeque(listOf(0))
    while (queue.isNotEmpty()) {
        val v = queue.removeFirst()
        if (v == target) return dist[v]
        for (u in adj[v]) if (dist[u] == 0) { dist[u] = dist[v] + 1; queue.addLast(u) }
    }
    return 0
}
