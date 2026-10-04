// 검증용 정답 (§6.1 solutions/). 글자마다 남은 개수를 들고 되추적한다.
fun tileSequences(tiles: String): Int {
    val counts = IntArray(26)
    for (ch in tiles) counts[ch - 'A'] += 1
    fun go(): Int {
        var total = 0
        for (c in 0 until 26) {
            if (counts[c] == 0) continue
            counts[c] -= 1
            Drill.push(c)
            total += 1 + go()
            counts[c] += 1
            Drill.pop(c)
        }
        return total
    }
    return go()
}
