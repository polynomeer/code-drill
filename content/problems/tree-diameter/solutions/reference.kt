// 검증용 정답 (§6.1 solutions/). 깊은 정점부터 "아래로 가장 긴 길이"를 부모에 올린다.
fun treeDiameter(parent: IntArray): Int {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    val path = IntArray(n)
    for (start in 0 until n) {
        var v = start
        var len = 0
        while (v != -1 && depth[v] == -1) { path[len++] = v; v = parent[v] }
        var d = if (v == -1) -1 else depth[v]
        while (len > 0) { d += 1; depth[path[--len]] = d }
    }
    val order = (0 until n).sortedByDescending { depth[it] }
    val down = IntArray(n)
    var best = 0
    for (v in order) {
        val p = parent[v]
        if (p == -1) continue
        val through = down[p] + down[v] + 1
        if (through > best) { best = through; Drill.match(p, through) }
        if (down[v] + 1 > down[p]) { down[p] = down[v] + 1; Drill.write(p, down[p]) }
    }
    return best
}
