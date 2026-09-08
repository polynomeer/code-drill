// kind: WRONG_ALGORITHM
// 나선이 아니라 행 순서대로 채운다.
fun spiralFill(n: Int): Array<IntArray> {
    val out = Array(n) { IntArray(n) }
    var value = 1
    for (r in 0 until n) {
        for (c in 0 until n) {
            out[r][c] = value
            value += 1
        }
    }
    return out
}
