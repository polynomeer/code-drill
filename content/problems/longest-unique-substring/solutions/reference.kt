// 검증용 정답 (§6.1 solutions/). 슬라이딩 윈도우 + 마지막 위치.
fun longestUnique(text: String): Int {
    val last = HashMap<Char, Int>()
    var start = 0
    var best = 0
    for (i in text.indices) {
        val seen = last[text[i]]
        if (seen != null && seen >= start) {
            start = seen + 1
            Drill.pointer("start", start)
        }
        last[text[i]] = i
        best = maxOf(best, i - start + 1)
        Drill.visit(i, best)
    }
    return best
}
