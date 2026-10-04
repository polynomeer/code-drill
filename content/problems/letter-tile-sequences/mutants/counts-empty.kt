// kind: OFF_BY_ONE
// 타일을 하나도 고르지 않은 빈 글자열까지 센다. 하나 이상 골라야 한다.
fun tileSequences(tiles: String): Int {
    val counts = IntArray(26)
    for (ch in tiles) counts[ch - 'A'] += 1
    fun go(): Int {
        var total = 1
        for (c in 0 until 26) {
            if (counts[c] == 0) continue
            counts[c] -= 1
            total += go()
            counts[c] += 1
        }
        return total
    }
    return go()
}
