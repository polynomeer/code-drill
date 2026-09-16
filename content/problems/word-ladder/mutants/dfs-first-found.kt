// kind: WRONG_ALGORITHM
// DFS 로 처음 닿은 길의 길이를 답한다. 가장 짧다는 보장이 없다.
fun wordLadder(begin: String, end: String, words: Array<String>): Int {
    val pool = words.toHashSet()
    if (end !in pool) return 0
    val seen = hashSetOf(begin)
    fun go(word: String, steps: Int): Int {
        if (word == end) return steps
        val chars = word.toCharArray()
        for (i in chars.indices) { val o = chars[i]; for (c in 'a'..'z') { if (c == o) continue; chars[i] = c; val n = String(chars); if (n in pool && seen.add(n)) { val r = go(n, steps + 1); if (r > 0) return r } }; chars[i] = o }
        return 0
    }
    return go(begin, 1)
}
