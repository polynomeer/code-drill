// kind: WRONG_BRANCH
// 짝수 개일 때 왼쪽 최댓값을 두 배 한다. 오른쪽 최솟값을 잊었다.
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val (x, y) = if (a.size <= b.size) a to b else b to a
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n + 1) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2; val j = half - i
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]; val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j == 0) Int.MIN_VALUE else y[j - 1]; val rightY = if (j == n) Int.MAX_VALUE else y[j]
        if (leftX <= rightY && leftY <= rightX) return 2 * maxOf(leftX, leftY)
        else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("")
}
