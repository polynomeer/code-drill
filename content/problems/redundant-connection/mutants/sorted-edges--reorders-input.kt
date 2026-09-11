// kind: WRONG_ALGORITHM
// 간선을 정렬해 처리한다. 어느 간선이 '처음' 순환을 만드는지가 달라진다.
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    val pairs = (0 until edges.size / 2).map { intArrayOf(edges[2 * it], edges[2 * it + 1]) }
        .sortedWith(compareBy({ it[0] }, { it[1] }))
    for (e in pairs) {
        val ra = find(e[0]); val rb = find(e[1])
        if (ra == rb) return intArrayOf(e[0], e[1])
        parent[ra] = rb
    }
    return intArrayOf(-1, -1)
}
