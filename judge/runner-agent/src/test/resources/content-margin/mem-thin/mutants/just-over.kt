// kind: PERFORMANCE
// 한도를 조금 넘게 잡는다.
fun total(n: Int): Int {
    val hog = IntArray(22_000_000)
    var s = 0
    for (i in 0 until n) s += i
    return s + hog[0]
}
