// kind: OFF_BY_ONE
// 마지막 항을 빠뜨린다.
fun total(n: Int): Int {
    var s = 0
    for (i in 0 until n - 1) s += i
    return s
}
