// kind: MISSING_EDGE_CASE
// Long 으로 바꿔 더한다. 자릿수가 19 를 넘으면 넘친다.
fun plusOne(digits: IntArray): IntArray {
    var n = 0L
    for (d in digits) n = n * 10 + d
    n += 1
    val s = n.toString()
    return IntArray(s.length) { s[it] - '0' }
}
