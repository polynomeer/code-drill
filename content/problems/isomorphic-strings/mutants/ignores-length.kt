// kind: MISSING_EDGE_CASE
// 길이가 다른 것을 확인하지 않는다. 짧은 쪽까지만 맞으면 같다고 본다.
fun isomorphic(first: String, second: String): Int {
    val forward = HashMap<Char, Char>()
    val backward = HashMap<Char, Char>()
    for (i in 0 until minOf(first.length, second.length)) {
        val a = first[i]; val b = second[i]
        val f = forward[a]; val g = backward[b]
        if ((f != null && f != b) || (g != null && g != a)) return 0
        forward[a] = b; backward[b] = a
    }
    return 1
}
