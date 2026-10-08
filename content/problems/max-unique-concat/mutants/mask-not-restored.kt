// kind: WRONG_BRANCH
// 한 갈래에서 넣은 글자를 돌아올 때 빼지 않는다. 다음 갈래가 쓰지도 않은 글자에 막힌다.
fun maxUniqueConcat(words: Array<String>): Int {
    val masks = ArrayList<Int>(); val lengths = ArrayList<Int>()
    for (w in words) {
        var m = 0; var ok = true
        for (ch in w) { val bit = 1 shl (ch - 'a'); if (m and bit != 0) { ok = false; break }; m = m or bit }
        if (ok) { masks.add(m); lengths.add(w.length) }
    }
    var best = 0
    var used = 0
    fun go(i: Int, length: Int) {
        if (length > best) best = length
        for (k in i until masks.size) {
            if (used and masks[k] == 0) { used = used or masks[k]; go(k + 1, length + lengths[k]) }
        }
    }
    go(0, 0)
    return best
}
