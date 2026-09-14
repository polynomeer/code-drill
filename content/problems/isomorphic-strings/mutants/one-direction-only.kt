// kind: MISSING_EDGE_CASE
// first 에서 second 로의 대응만 기억한다. 서로 다른 문자가 같은 문자로 바뀌는 것을 놓친다.
fun isomorphic(first: String, second: String): Int {
    if (first.length != second.length) return 0
    val forward = HashMap<Char, Char>()
    for (i in first.indices) {
        val f = forward[first[i]]
        if (f != null && f != second[i]) return 0
        forward[first[i]] = second[i]
    }
    return 1
}
