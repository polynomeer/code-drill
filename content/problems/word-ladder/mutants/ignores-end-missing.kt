// kind: MISSING_EDGE_CASE
// end 가 목록에 없어도 한 글자 차이면 간다.
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = words.toHashSet().apply { add(end) }
    val seen = hashSetOf(begin)
    val queue = ArrayDeque(listOf(begin to 1))
    while (queue.isNotEmpty()) {
        val (word, steps) = queue.removeFirst()
        if (word == end) return steps
        val chars = word.toCharArray()
        for (i in chars.indices) { val o = chars[i]; for (c in 'a'..'z') { if (c == o) continue; chars[i] = c; val n = String(chars); if (n in pool && seen.add(n)) queue.addLast(n to steps + 1) }; chars[i] = o }
    }
    return 0
}
