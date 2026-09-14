// 검증용 정답 (§6.1 solutions/). 두 방향의 대응을 함께 기억한다.
fun isomorphic(first: String, second: String): Int {
    if (first.length != second.length) return 0
    val forward = HashMap<Char, Char>()
    val backward = HashMap<Char, Char>()
    for (i in first.indices) {
        val a = first[i]
        val b = second[i]
        Drill.visit(i, 0)
        val f = forward[a]
        val g = backward[b]
        if ((f != null && f != b) || (g != null && g != a)) return 0
        forward[a] = b
        backward[b] = a
        Drill.match(i, 1)
    }
    return 1
}
