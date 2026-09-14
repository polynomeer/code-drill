// 검증용 정답 (§6.1 solutions/). 깊이를 한 번씩만 정한다 — 올라가다 정해진 정점을 만나면 멈춘다.
fun levelSums(parent: IntArray, values: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    val path = IntArray(n)
    var height = 0
    for (start in 0 until n) {
        var v = start
        var len = 0
        while (v != -1 && depth[v] == -1) { path[len++] = v; v = parent[v] }
        var d = if (v == -1) -1 else depth[v]
        while (len > 0) {
            val u = path[--len]
            d += 1
            depth[u] = d
            Drill.node("v$u")
            if (d > height) height = d
        }
    }
    val sums = IntArray(height + 1)
    for (v in 0 until n) {
        sums[depth[v]] += values[v]
        Drill.write(depth[v], sums[depth[v]])
    }
    return sums
}
