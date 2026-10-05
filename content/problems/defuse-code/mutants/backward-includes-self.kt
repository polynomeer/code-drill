// kind: OFF_BY_ONE
// k 가 음수일 때 자기 자신까지 더하고 가장 먼 하나를 뺀다. 창은 바로 앞에서 끝난다.
fun defuseCode(code: IntArray, k: Int): IntArray {
    val n = code.size
    val out = IntArray(n)
    if (k == 0) return out
    val lo = if (k > 0) 1 else n + k + 1
    val hi = if (k > 0) k else n
    var window = 0
    for (j in lo..hi) window += code[j % n]
    for (i in 0 until n) { out[i] = window; window -= code[(i + lo) % n]; window += code[(i + hi + 1) % n] }
    return out
}
