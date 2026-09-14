// kind: WRONG_BRANCH
// 점프를 다 한 뒤 마지막으로 부모 한 칸을 올리지 않는다. 공통 조상의 자식을 답한다.
fun lowestCommonAncestors(parent: IntArray, queries: IntArray): IntArray {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    fun depthOf(v: Int): Int { if (depth[v] == -1) depth[v] = if (parent[v] == -1) 0 else depthOf(parent[v]) + 1; return depth[v] }
    for (v in 0 until n) depthOf(v)
    var log = 1
    while ((1 shl log) < n) log += 1
    val up = Array(log) { IntArray(n) }
    for (v in 0 until n) up[0][v] = if (parent[v] == -1) v else parent[v]
    for (k in 1 until log) for (v in 0 until n) up[k][v] = up[k - 1][up[k - 1][v]]
    val out = IntArray(queries.size / 2)
    for (i in out.indices) {
        var a = queries[2 * i]; var b = queries[2 * i + 1]
        if (depth[a] < depth[b]) { val t = a; a = b; b = t }
        var diff = depth[a] - depth[b]
        var k = 0
        while (diff > 0) { if (diff and 1 == 1) a = up[k][a]; diff = diff shr 1; k += 1 }
        if (a != b) {
            for (j in log - 1 downTo 0) if (up[j][a] != up[j][b]) { a = up[j][a]; b = up[j][b] }
        }
        out[i] = a
    }
    return out
}
