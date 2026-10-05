// kind: WRONG_BRANCH
// 답을 정렬한 질문의 순서대로 담는다. 답은 원래 질문 순서다.
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val m = edges.size / 3; val q = queries.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val asked = (0 until q).sortedBy { queries[3 * it + 2] }
    val out = IntArray(q)
    var k = 0
    for ((slot, i) in asked.withIndex()) {
        while (k < m && edges[3 * order[k] + 2] < queries[3 * i + 2]) { val e = order[k]; val a = find(edges[3 * e]); val b = find(edges[3 * e + 1]); if (a != b) parent[a] = b; k += 1 }
        out[slot] = if (find(queries[3 * i]) == find(queries[3 * i + 1])) 1 else 0
    }
    return out
}
