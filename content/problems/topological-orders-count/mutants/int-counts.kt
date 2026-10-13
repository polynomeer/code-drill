// kind: MISSING_EDGE_CASE
// 방법의 수를 나머지 없이 Int 로 더한다. 13! 부터 Int 를 넘는다.
fun topologicalOrdersCount(n: Int, edges: IntArray): Int {
    val need = IntArray(n)
    for (i in edges.indices step 2) need[edges[i + 1]] = need[edges[i + 1]] or (1 shl edges[i])
    val ways = IntArray(1 shl n); ways[0] = 1
    for (mask in 0 until (1 shl n)) {
        if (ways[mask] == 0) continue
        for (v in 0 until n) if (mask shr v and 1 == 0 && need[v] and mask == need[v]) ways[mask or (1 shl v)] += ways[mask]
    }
    return ways[(1 shl n) - 1]
}
