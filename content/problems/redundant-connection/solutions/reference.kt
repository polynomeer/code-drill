// 검증용 정답 (§6.1 solutions/). 유니온파인드, 경로 압축.
fun redundantEdge(n: Int, edges: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        Drill.node(v.toString())
        return v
    }
    var i = 0
    while (i < edges.size) {
        val a = edges[i]; val b = edges[i + 1]
        Drill.edge(a.toString(), b.toString())
        val ra = find(a); val rb = find(b)
        if (ra == rb) { Drill.match(a, b); return intArrayOf(a, b) }
        parent[ra] = rb
        i += 2
    }
    return intArrayOf(-1, -1)
}
