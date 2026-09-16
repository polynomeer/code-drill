// 검증용 정답 (§6.1 solutions/). 세 포인터.
fun nthUglyNumber(n: Int): Int {
    val ugly = IntArray(n)
    ugly[0] = 1
    var i2 = 0; var i3 = 0; var i5 = 0
    for (k in 1 until n) {
        val c2 = ugly[i2] * 2; val c3 = ugly[i3] * 3; val c5 = ugly[i5] * 5
        Drill.compare(c2, c3); Drill.compare(c3, c5)
        val next = minOf(c2, minOf(c3, c5))
        ugly[k] = next
        Drill.write(k, next)
        if (next == c2) i2 += 1
        if (next == c3) i3 += 1
        if (next == c5) i5 += 1
    }
    return ugly[n - 1]
}
