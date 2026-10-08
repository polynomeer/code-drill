// 검증용 정답 (§6.1 solutions/). 낱말을 비트로 적고, 넣는 갈래와 넣지 않는 갈래를 모두 해 본다.
fun maxUniqueConcat(words: Array<String>): Int {
    val masks = ArrayList<Int>()
    val lengths = ArrayList<Int>()
    for (w in words) {
        var m = 0
        var ok = true
        for (ch in w) {
            val bit = 1 shl (ch - 'a')
            if (m and bit != 0) { ok = false; break }
            m = m or bit
        }
        if (ok) { masks.add(m); lengths.add(w.length) }
    }
    var best = 0
    fun go(i: Int, used: Int, length: Int) {
        if (length > best) best = length
        for (k in i until masks.size) {
            if (used and masks[k] == 0) {
                Drill.push(k)
                go(k + 1, used or masks[k], length + lengths[k])
                Drill.pop(k)
            }
        }
    }
    go(0, 0, 0)
    return best
}
