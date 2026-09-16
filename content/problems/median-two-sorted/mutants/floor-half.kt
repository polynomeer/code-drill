// kind: OFF_BY_ONE
// 왼쪽 절반의 크기를 (m+n)/2 로 잡는다. 홀수 개일 때 가운데가 오른쪽으로 간다.
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val (x, y) = if (a.size <= b.size) a to b else b to a
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2; val j = half - i
        if (j < 0) { hi = i - 1; continue }
        if (j > n) { lo = i + 1; continue }
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]; val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j == 0) Int.MIN_VALUE else y[j - 1]; val rightY = if (j == n) Int.MAX_VALUE else y[j]
        if (leftX <= rightY && leftY <= rightX) {
            val leftMax = maxOf(leftX, leftY)
            if ((m + n) % 2 == 1) return 2 * leftMax
            return leftMax + minOf(rightX, rightY)
        } else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("")
}
