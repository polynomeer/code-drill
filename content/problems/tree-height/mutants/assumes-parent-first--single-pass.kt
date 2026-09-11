// kind: MISSING_EDGE_CASE
// 부모가 자식보다 먼저 온다고 믿고 한 번만 훑는다. 루트가 뒤에 있으면 틀린다.
fun treeHeight(parent: IntArray): Int {
    val depth = IntArray(parent.size)
    var best = 0
    for (i in parent.indices) {
        depth[i] = if (parent[i] == -1) 0 else depth[parent[i]] + 1
        if (depth[i] > best) best = depth[i]
    }
    return best
}
