// kind: OFF_BY_ONE
// 같은 간선을 볼 때마다 진입 차수를 올린다. 두 번 나온 간선의 끝 글자가 영원히 안 나온다.
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
        after[x][y] = true; indegree[y] += 1
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) { val c = heap.poll(); out.append('a' + c); for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
