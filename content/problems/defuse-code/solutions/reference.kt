// 검증용 정답 (§6.1 solutions/). 창 하나를 한 칸씩 밀며 앞에서 빼고 뒤에서 더한다.
fun defuseCode(code: IntArray, k: Int): IntArray {
    val n = code.size
    val out = IntArray(n)
    if (k == 0) return out
    val lo = if (k > 0) 1 else n + k
    val hi = if (k > 0) k else n - 1
    var window = 0
    for (j in lo..hi) window += code[j % n]
    for (i in 0 until n) {
        out[i] = window
        Drill.pointer("from", (i + lo) % n)
        Drill.write(i, window)
        window -= code[(i + lo) % n]
        window += code[(i + hi + 1) % n]
    }
    return out
}
