// 검증용 정답 (§6.1 solutions/). 경계를 좁혀 가며 값을 채운다.
//
// 홀수 크기의 한가운데 한 칸이 함정이다. 네 방향을 무조건 다 돌면 그 칸을 두 번 쓴다.
// `top <= bottom` 과 `left <= right` 로 남은 줄이 있을 때만 돈다.
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var top = 0
    var bottom = n - 1
    var left = 0
    var right = n - 1
    var value = 1

    while (value <= n * n) {
        for (c in left..right) {
            out[top][c] = value
            Drill.write(top * n + c, value)
            value += 1
        }
        top += 1

        for (r in top..bottom) {
            out[r][right] = value
            Drill.write(r * n + right, value)
            value += 1
        }
        right -= 1

        if (top <= bottom) {
            for (c in right downTo left) {
                out[bottom][c] = value
                Drill.write(bottom * n + c, value)
                value += 1
            }
            bottom -= 1
        }

        if (left <= right) {
            for (r in bottom downTo top) {
                out[r][left] = value
                Drill.write(r * n + left, value)
                value += 1
            }
            left += 1
        }
    }
    return out
}
