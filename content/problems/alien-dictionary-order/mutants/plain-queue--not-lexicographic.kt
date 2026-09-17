// kind: WRONG_BRANCH
// 최소 힙 대신 보통 큐를 쓴다. 답이 여럿일 때 사전순 최소가 아니다.
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
        if (!after[x][y]) { after[x][y] = true; indegree[y] += 1 }
    }
    val queue = ArrayDeque<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) queue.addLast(c)
    val out = StringBuilder()
    while (queue.isNotEmpty()) { val c = queue.removeFirst(); out.append('a' + c); for (d in 25 downTo 0) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) queue.addLast(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
