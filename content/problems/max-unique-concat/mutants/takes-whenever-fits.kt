// kind: WRONG_ALGORITHM
// 넣을 수 있는 낱말은 늘 넣는다. 짧은 낱말 하나가 긴 낱말 여럿을 막을 수 있다.
fun maxUniqueConcat(words: Array<String>): Int {
    var used = 0
    var length = 0
    for (w in words) {
        var m = 0; var ok = true
        for (ch in w) { val bit = 1 shl (ch - 'a'); if (m and bit != 0) { ok = false; break }; m = m or bit }
        if (ok && used and m == 0) { used = used or m; length += w.length }
    }
    return length
}
