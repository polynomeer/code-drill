// kind: MISSING_EDGE_CASE
// 루트는 잎으로 세지 않는다. 정점이 하나뿐이면 루트가 잎이다.
fun countLeaves(parent: IntArray): Int {
    val hasChild = BooleanArray(parent.size)
    for (p in parent) if (p != -1) hasChild[p] = true
    return parent.indices.count { !hasChild[it] && parent[it] != -1 }
}
