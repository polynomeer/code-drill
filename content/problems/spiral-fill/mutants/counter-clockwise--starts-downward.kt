// kind: WRONG_BRANCH
// 아래로 먼저 내려가는 반시계 나선을 만든다.
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var top = 0
    var bottom = n - 1
    var left = 0
    var right = n - 1
    var value = 1
    while (value <= n * n) {
        for (r in top..bottom) { out[r][left] = value; value += 1 }
        left += 1
        for (c in left..right) { out[bottom][c] = value; value += 1 }
        bottom -= 1
        if (left <= right) {
            for (r in bottom downTo top) { out[r][right] = value; value += 1 }
            right -= 1
        }
        if (top <= bottom) {
            for (c in right downTo left) { out[top][c] = value; value += 1 }
            top += 1
        }
    }
    return out
}
