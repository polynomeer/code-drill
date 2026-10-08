// 검증용 정답 (§6.1 solutions/). 부모로 나온 정점을 표시하고, 표시되지 않은 정점을 센다.
fun countLeaves(parent: IntArray): Int {
    val hasChild = BooleanArray(parent.size)
    for (p in parent) if (p != -1) hasChild[p] = true
    var count = 0
    for (v in parent.indices) if (!hasChild[v]) { count += 1; Drill.node(v.toString()) }
    return count
}
