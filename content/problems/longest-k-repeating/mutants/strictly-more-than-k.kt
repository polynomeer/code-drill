// kind: OFF_BY_ONE
// k 번보다 많아야 남긴다. 정확히 k 번도 된다.
fun longestKRepeating(s: String, k: Int): Int {
    var best = 0
    val stack = ArrayDeque<IntArray>()
    stack.addLast(intArrayOf(0, s.length))
    while (stack.isNotEmpty()) {
        val (low, high) = stack.removeLast().let { it[0] to it[1] }
        if (high - low <= best) continue
        val counts = IntArray(26)
        for (i in low until high) counts[s[i] - 'a'] += 1
        var previous = low
        var cut = false
        for (i in low until high) if (counts[s[i] - 'a'] <= k) { stack.addLast(intArrayOf(previous, i)); previous = i + 1; cut = true }
        if (!cut) best = high - low else stack.addLast(intArrayOf(previous, high))
    }
    return best
}
