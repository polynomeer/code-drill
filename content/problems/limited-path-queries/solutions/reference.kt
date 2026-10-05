// 검증용 정답 (§6.1 solutions/). 질문과 간선을 무게 순으로 정렬하고 가벼운 간선부터 유니온 파인드에 더한다.
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    val m = edges.size / 3
    val q = queries.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val asked = (0 until q).sortedBy { queries[3 * it + 2] }
    val out = IntArray(q)
    var k = 0
    for (i in asked) {
        val limit = queries[3 * i + 2]
        while (k < m && edges[3 * order[k] + 2] < limit) {
            val e = order[k]
            val a = find(edges[3 * e]); val b = find(edges[3 * e + 1])
            if (a != b) {
                parent[a] = b
                Drill.edge(edges[3 * e].toString(), edges[3 * e + 1].toString())
            }
            k += 1
        }
        out[i] = if (find(queries[3 * i]) == find(queries[3 * i + 1])) 1 else 0
        Drill.match(queries[3 * i], queries[3 * i + 1])
    }
    return out
}
