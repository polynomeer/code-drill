// kind: WRONG_ALGORITHM
// 같은 글자 타일을 서로 다른 것으로 센다. 두 A 를 맞바꾼 것이 따로 세어진다.
fun tileSequences(tiles: String): Int {
    val used = BooleanArray(tiles.length)
    fun go(): Int {
        var total = 0
        for (i in tiles.indices) {
            if (used[i]) continue
            used[i] = true
            total += 1 + go()
            used[i] = false
        }
        return total
    }
    return go()
}
