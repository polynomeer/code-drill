// 검증용 정답 (§6.1 solutions/). 인접 쌍에서 간선, 최소 힙 Kahn.
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        while (j < a.length && j < b.length && a[j] == b[j]) j += 1
        if (j == minOf(a.length, b.length)) { if (a.length > b.length) return ""; continue }
        val x = a[j] - 'a'; val y = b[j] - 'a'
        if (!after[x][y]) { after[x][y] = true; indegree[y] += 1; Drill.compare(x, y) }
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) {
        val c = heap.poll()
        Drill.write(out.length, c)
        out.append('a' + c)
        for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) }
    }
    val total = present.count { it }
    return if (out.length == total) out.toString() else ""
}
