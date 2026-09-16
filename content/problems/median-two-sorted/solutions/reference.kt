// 검증용 정답 (§6.1 solutions/). 짧은 배열에서 자르는 자리를 이분 탐색한다.
fun medianDoubled(a: IntArray, b: IntArray): Int {
    val (x, y) = if (a.size <= b.size) a to b else b to a
    val m = x.size; val n = y.size
    var lo = 0; var hi = m
    val half = (m + n + 1) / 2
    while (lo <= hi) {
        val i = (lo + hi) / 2
        val j = half - i
        Drill.pointer("cut", i)
        val leftX = if (i == 0) Int.MIN_VALUE else x[i - 1]
        val rightX = if (i == m) Int.MAX_VALUE else x[i]
        val leftY = if (j == 0) Int.MIN_VALUE else y[j - 1]
        val rightY = if (j == n) Int.MAX_VALUE else y[j]
        Drill.compare(i, j)
        if (leftX <= rightY && leftY <= rightX) {
            val leftMax = maxOf(leftX, leftY)
            if ((m + n) % 2 == 1) return 2 * leftMax
            return leftMax + minOf(rightX, rightY)
        } else if (leftX > rightY) hi = i - 1 else lo = i + 1
    }
    error("정렬된 입력이면 여기 오지 않는다")
}
