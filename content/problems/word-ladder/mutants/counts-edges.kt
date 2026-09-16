// kind: OFF_BY_ONE
// 변환의 횟수를 답한다. 단어 수는 하나 더 많다.
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = words.toHashSet()
    if (end !in pool) return 0
    val seen = hashSetOf(begin)
    val queue = ArrayDeque(listOf(begin to 0))
    while (queue.isNotEmpty()) {
        val (word, steps) = queue.removeFirst()
        if (word == end) return steps
        val chars = word.toCharArray()
        for (i in chars.indices) { val o = chars[i]; for (c in 'a'..'z') { if (c == o) continue; chars[i] = c; val n = String(chars); if (n in pool && seen.add(n)) queue.addLast(n to steps + 1) }; chars[i] = o }
    }
    return 0
}
