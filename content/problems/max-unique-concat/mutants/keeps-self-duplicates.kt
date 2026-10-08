// kind: MISSING_EDGE_CASE
// 자기 안에서 글자가 겹치는 낱말도 쓴다. 비트로 적으면 그 겹침이 보이지 않는다.
fun maxUniqueConcat(words: Array<String>): Int {
    val masks = words.map { w -> w.fold(0) { m, ch -> m or (1 shl (ch - 'a')) } }
    var best = 0
    fun go(i: Int, used: Int, length: Int) {
        if (length > best) best = length
        for (k in i until words.size) if (used and masks[k] == 0) go(k + 1, used or masks[k], length + words[k].length)
    }
    go(0, 0, 0)
    return best
}
