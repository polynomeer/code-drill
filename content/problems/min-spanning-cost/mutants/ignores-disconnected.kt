// kind: MISSING_EDGE_CASE
// 간선을 다 보고도 n-1 개를 못 골랐는데 비용을 돌려준다. 이을 수 없으면 -1 이다.
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var total = 0
    for (i in order) {
        val ra = find(edges[3 * i]); val rb = find(edges[3 * i + 1])
        if (ra == rb) continue
        parent[ra] = rb
        total += edges[3 * i + 2]
    }
    return total
}
