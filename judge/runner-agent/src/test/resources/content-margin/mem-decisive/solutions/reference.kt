// 검증용 정답 (§6.1 solutions/).
fun total(n: Int): Int {
    var s = 0
    for (i in 0 until n) s += i
    return s
}
