// 검증용 정답 (§6.1 solutions/). 이진 리프팅 — 2^k 번째 조상 표.
fun lowestCommonAncestors(parent: IntArray, queries: IntArray): IntArray {
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
    var log = 1
    while ((1 shl log) < n) log += 1
    val up = Array(log) { IntArray(n) }
    for (v in 0 until n) up[0][v] = if (parent[v] == -1) v else parent[v]
    for (k in 1 until log) for (v in 0 until n) up[k][v] = up[k - 1][up[k - 1][v]]

    val out = IntArray(queries.size / 2)
    for (i in out.indices) {
        var a = queries[2 * i]
        var b = queries[2 * i + 1]
        Drill.compare(a, b)
        if (depth[a] < depth[b]) { val t = a; a = b; b = t }
        var diff = depth[a] - depth[b]
        var k = 0
        while (diff > 0) { if (diff and 1 == 1) a = up[k][a]; diff = diff shr 1; k += 1 }
        if (a != b) {
            for (j in log - 1 downTo 0) if (up[j][a] != up[j][b]) { a = up[j][a]; b = up[j][b] }
            a = parent[a]
        }
        Drill.match(a, a)
        out[i] = a
    }
    return out
}
