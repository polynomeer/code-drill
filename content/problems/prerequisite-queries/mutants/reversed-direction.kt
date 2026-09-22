// kind: WRONG_BRANCH
// 방향을 거꾸로 답한다 — v 가 u 의 선수인가.
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    val incoming = Array(n) { ArrayList<Int>() }
    val indegree = IntArray(n)
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); incoming[edges[i + 1]].add(edges[i]); indegree[edges[i + 1]] += 1 }
    val order = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (indegree[v] == 0) order[tail++] = v
    while (head < tail) { val u = order[head++]; for (v in adj[u]) { indegree[v] -= 1; if (indegree[v] == 0) order[tail++] = v } }
    val words = (n + 63) / 64
    val reach = Array(n) { LongArray(words) }
    for (idx in 0 until n) { val v = order[idx]; for (u in incoming[v]) { val ru = reach[u]; val rv = reach[v]; for (w in 0 until words) rv[w] = rv[w] or ru[w]; rv[u / 64] = rv[u / 64] or (1L shl (u % 64)) } }
    return IntArray(queries.size / 2) { q -> val u = queries[2 * q]; val v = queries[2 * q + 1]; if ((reach[u][v / 64] ushr (v % 64)) and 1L == 1L) 1 else 0 }
}
