// kind: OFF_BY_ONE
// 무게가 limit 과 같은 간선도 쓴다. 엄격히 작아야 한다.
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val m = edges.size / 3; val q = queries.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val asked = (0 until q).sortedBy { queries[3 * it + 2] }
    val out = IntArray(q)
    var k = 0
    for (i in asked) {
        while (k < m && edges[3 * order[k] + 2] <= queries[3 * i + 2]) { val e = order[k]; val a = find(edges[3 * e]); val b = find(edges[3 * e + 1]); if (a != b) parent[a] = b; k += 1 }
        out[i] = if (find(queries[3 * i]) == find(queries[3 * i + 1])) 1 else 0
    }
    return out
}
