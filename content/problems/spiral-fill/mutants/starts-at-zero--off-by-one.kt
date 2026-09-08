// kind: OFF_BY_ONE
// `0` 부터 채워 마지막 값이 하나씩 모자란다.
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var top = 0
    var bottom = n - 1
    var left = 0
    var right = n - 1
    var value = 0
    while (value < n * n) {
        for (c in left..right) { out[top][c] = value; value += 1 }
        top += 1
        for (r in top..bottom) { out[r][right] = value; value += 1 }
        right -= 1
        if (top <= bottom) {
            for (c in right downTo left) { out[bottom][c] = value; value += 1 }
            bottom -= 1
        }
        if (left <= right) {
            for (r in bottom downTo top) { out[r][left] = value; value += 1 }
            left += 1
        }
    }
    return out
}
