// kind: WRONG_BRANCH
// 고른 글자를 돌아올 때 되돌려 놓지 않는다. 한 갈래가 쓴 타일이 다음 갈래에서 사라진다.
fun tileSequences(tiles: String): Int {
    val counts = IntArray(26)
    for (ch in tiles) counts[ch - 'A'] += 1
    fun go(): Int {
        var total = 0
        for (c in 0 until 26) {
            if (counts[c] == 0) continue
            counts[c] -= 1
            total += 1 + go()
        }
        return total
    }
    return go()
}
