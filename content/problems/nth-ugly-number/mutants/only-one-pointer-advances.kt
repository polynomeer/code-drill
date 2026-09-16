// kind: WRONG_BRANCH
// 가장 작은 곱을 만든 포인터 하나만 나아간다. 6 이 두 번 나온다.
fun nthUglyNumber(n: Int): Int {
    val ugly = IntArray(n)
    ugly[0] = 1
    var i2 = 0; var i3 = 0; var i5 = 0
    for (k in 1 until n) {
        val c2 = ugly[i2] * 2; val c3 = ugly[i3] * 3; val c5 = ugly[i5] * 5
        val next = minOf(c2, minOf(c3, c5))
        ugly[k] = next
        if (next == c2) i2 += 1 else if (next == c3) i3 += 1 else i5 += 1
    }
    return ugly[n - 1]
}
