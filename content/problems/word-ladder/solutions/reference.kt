// 검증용 정답 (§6.1 solutions/). 자리마다 26 글자를 바꿔 목록에 있는지 본다. BFS.
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = HashSet<String>()
    val index = HashMap<String, Int>()
    for ((i, w) in words.withIndex()) { pool.add(w); index[w] = i }
    if (end !in pool) return 0
    val seen = HashSet<String>().apply { add(begin) }
    val queue = ArrayDeque<Pair<String, Int>>()
    queue.addLast(begin to 1)
    while (queue.isNotEmpty()) {
        val (word, steps) = queue.removeFirst()
        Drill.dequeue(index[word] ?: -1)
        if (word == end) { Drill.match(index[word] ?: -1, steps); return steps }
        val chars = word.toCharArray()
        for (i in chars.indices) {
            val original = chars[i]
            for (c in 'a'..'z') {
                if (c == original) continue
                chars[i] = c
                val next = String(chars)
                if (next in pool && seen.add(next)) { queue.addLast(next to steps + 1); Drill.enqueue(index[next] ?: -1) }
            }
            chars[i] = original
        }
    }
    return 0
}
