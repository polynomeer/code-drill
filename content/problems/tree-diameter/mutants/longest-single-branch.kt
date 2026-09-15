// kind: WRONG_BRANCH
// 정점마다 아래로 가장 긴 가지 하나만 본다. 두 가지를 합치지 않는다.
fun treeDiameter(parent: IntArray): Int {
    val n = parent.size
    val depth = IntArray(n) { -1 }
    val path = IntArray(n)
    for (start in 0 until n) {
        var v = start; var len = 0
        while (v != -1 && depth[v] == -1) { path[len++] = v; v = parent[v] }
        var d = if (v == -1) -1 else depth[v]
        while (len > 0) { d += 1; depth[path[--len]] = d }
    }
    return depth.max()
}
