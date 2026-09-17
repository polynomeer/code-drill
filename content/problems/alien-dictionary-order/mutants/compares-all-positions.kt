// kind: WRONG_ALGORITHM
// 처음 다른 글자 뒤의 자리들도 순서로 삼는다. 없는 제약이 생겨 모순이 난다.
fun alienDictionaryOrder(words: Array<String>): String {
    val present = BooleanArray(26)
    for (w in words) for (c in w) present[c - 'a'] = true
    val after = Array(26) { BooleanArray(26) }
    val indegree = IntArray(26)
    for (i in 0 until words.size - 1) {
        val a = words[i]; val b = words[i + 1]
        var j = 0
        var differed = false
        while (j < a.length && j < b.length) {
            if (a[j] != b[j]) { differed = true; val x = a[j] - 'a'; val y = b[j] - 'a'; if (!after[x][y]) { after[x][y] = true; indegree[y] += 1 } }
            j += 1
        }
        if (!differed && a.length > b.length) return ""
    }
    val heap = java.util.PriorityQueue<Int>()
    for (c in 0 until 26) if (present[c] && indegree[c] == 0) heap.add(c)
    val out = StringBuilder()
    while (heap.isNotEmpty()) { val c = heap.poll(); out.append('a' + c); for (d in 0 until 26) if (after[c][d]) { indegree[d] -= 1; if (indegree[d] == 0) heap.add(d) } }
    return if (out.length == present.count { it }) out.toString() else ""
}
