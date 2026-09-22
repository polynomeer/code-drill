// kind: MISSING_EDGE_CASE
// 정점을 번호 순으로 처리한다. 간선이 번호 역순이면 도달 집합이 덜 찬다.
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val incoming = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) incoming[edges[i + 1]].add(edges[i])
    val words = (n + 63) / 64
    val reach = Array(n) { LongArray(words) }
    for (v in n - 1 downTo 0) { for (u in incoming[v]) { val ru = reach[u]; val rv = reach[v]; for (w in 0 until words) rv[w] = rv[w] or ru[w]; rv[u / 64] = rv[u / 64] or (1L shl (u % 64)) } }
    return IntArray(queries.size / 2) { q -> val u = queries[2 * q]; val v = queries[2 * q + 1]; if ((reach[v][u / 64] ushr (u % 64)) and 1L == 1L) 1 else 0 }
}
