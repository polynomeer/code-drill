// kind: OFF_BY_ONE
// 1 을 못생긴 수로 세지 않는다. 답이 한 자리 밀린다.
fun nthUglyNumber(n: Int): Int {
    val ugly = IntArray(n + 1)
    ugly[0] = 1
    var i2 = 0; var i3 = 0; var i5 = 0
    for (k in 1..n) {
        val c2 = ugly[i2] * 2; val c3 = ugly[i3] * 3; val c5 = ugly[i5] * 5
        val next = minOf(c2, minOf(c3, c5))
        ugly[k] = next
        if (next == c2) i2 += 1
        if (next == c3) i3 += 1
        if (next == c5) i5 += 1
    }
    return ugly[n]
}
