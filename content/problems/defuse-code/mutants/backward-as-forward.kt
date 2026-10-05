// kind: WRONG_BRANCH
// k 가 음수여도 다음 자리들을 더한다. 음수면 앞 자리들이다.
fun defuseCode(code: IntArray, k: Int): IntArray {
    val n = code.size
    val out = IntArray(n)
    if (k == 0) return out
    val width = if (k > 0) k else -k
    var window = 0
    for (j in 1..width) window += code[j % n]
    for (i in 0 until n) { out[i] = window; window -= code[(i + 1) % n]; window += code[(i + width + 1) % n] }
    return out
}
