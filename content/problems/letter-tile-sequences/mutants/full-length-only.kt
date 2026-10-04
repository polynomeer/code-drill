// kind: WRONG_BRANCH
// 타일을 모두 쓴 글자열만 센다. 몇 개만 골라도 한 가지다.
fun tileSequences(tiles: String): Int {
    val counts = IntArray(26)
    for (ch in tiles) counts[ch - 'A'] += 1
    fun go(left: Int): Int {
        if (left == 0) return 1
        var total = 0
        for (c in 0 until 26) {
            if (counts[c] == 0) continue
            counts[c] -= 1
            total += go(left - 1)
            counts[c] += 1
        }
        return total
    }
    return go(tiles.length)
}
