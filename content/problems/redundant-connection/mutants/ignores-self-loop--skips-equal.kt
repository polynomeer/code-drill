// kind: MISSING_EDGE_CASE
// 양 끝이 같은 간선을 건너뛴다. 자기 자신을 잇는 간선도 순환이다.
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) v = parent[v]; return v }
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        if (a != b) {
            val ra = find(a); val rb = find(b)
            if (ra == rb) return intArrayOf(a, b)
            parent[ra] = rb
        }
        i += 2
    }
    return intArrayOf(-1, -1)
}
