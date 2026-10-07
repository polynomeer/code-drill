// 검증용 정답 (§6.1 solutions/). 너비 우선 순서를 거꾸로 훑어 자식의 이득부터 정한다 — 재귀 없이.
fun treeMaxPathSum(parent: IntArray, values: IntArray): Int {
    val n = parent.size
    val head = IntArray(n) { -1 }
    val next = IntArray(n)
    var root = 0
    for (v in 0 until n) {
        val p = parent[v]
        if (p == -1) root = v else { next[v] = head[p]; head[p] = v }
    }
    val order = IntArray(n)
    order[0] = root
    var tail = 1
    for (i in 0 until n) {
        var c = head[order[i]]
        while (c != -1) { order[tail] = c; tail += 1; c = next[c] }
    }
    val gain = IntArray(n)
    var best = Int.MIN_VALUE
    for (i in n - 1 downTo 0) {
        val v = order[i]
        var first = 0
        var second = 0
        var c = head[v]
        while (c != -1) {
            val g = gain[c]
            if (g > first) { second = first; first = g } else if (g > second) second = g
            c = next[c]
        }
        gain[v] = values[v] + first
        Drill.write(v, gain[v])
        best = maxOf(best, values[v] + first + second)
    }
    return best
}
