// 검증용 정답 (§6.1 solutions/). 구간 전체에서 k 번 미만인 글자 자리에서 자르고, 조각마다 같은 질문을 한다 — 스택으로.
fun longestKRepeating(s: String, k: Int): Int {
    var best = 0
    val stack = ArrayDeque<IntArray>()
    stack.addLast(intArrayOf(0, s.length))
    while (stack.isNotEmpty()) {
        val (low, high) = stack.removeLast().let { it[0] to it[1] }
        if (high - low < k || high - low <= best) continue
        val counts = IntArray(26)
        for (i in low until high) counts[s[i] - 'a'] += 1
        Drill.call("$low..$high")
        var previous = low
        var cut = false
        for (i in low until high) {
            if (counts[s[i] - 'a'] < k) {
                stack.addLast(intArrayOf(previous, i))
                previous = i + 1
                cut = true
            }
        }
        if (!cut) { best = high - low; Drill.ret("$low..$high", best) }
        else stack.addLast(intArrayOf(previous, high))
    }
    return best
}
