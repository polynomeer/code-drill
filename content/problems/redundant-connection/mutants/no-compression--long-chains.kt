// kind: PERFORMANCE
// 경로 압축도 랭크도 없다. 사슬이 길어지면 찾기가 O(n).
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var v = x; while (parent[v] != v) { Drill.edge(v.toString(), parent[v].toString()); v = parent[v] }; return v }
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        val ra = find(a); val rb = find(b)
        if (ra == rb) return intArrayOf(a, b)
        parent[rb] = ra
        i += 2
    }
    return intArrayOf(-1, -1)
}
