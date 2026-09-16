// kind: MISSING_EDGE_CASE
// 짧은 배열을 고르지 않는다. j 가 음수가 되어 범위 밖을 읽거나 틀린다.
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val x = a; val y = b
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n + 1) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2; val j = half - i
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]; val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j <= 0) Int.MIN_VALUE else y[j - 1]; val rightY = if (j >= n) Int.MAX_VALUE else y[j]
        if (leftX <= rightY && leftY <= rightX) {
            val leftMax = maxOf(leftX, leftY)
            if ((m + n) % 2 == 1) return 2 * leftMax
            return leftMax + minOf(rightX, rightY)
        } else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("")
}
