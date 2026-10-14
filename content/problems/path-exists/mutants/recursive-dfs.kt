// kind: MISSING_EDGE_CASE
// 재귀 DFS 로 찾는다. 20 만 정점이 한 줄이면 스택이 넘친다.
fun pathExists(n: Int, edges: IntArray, source: Int, target: Int): Int {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val seen = BooleanArray(n)
    fun go(u: Int) { seen[u] = true; for (v in adj[u]) if (!seen[v]) go(v) }
    go(source)
    return if (seen[target]) 1 else 0
}
