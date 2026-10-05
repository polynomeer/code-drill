// kind: PERFORMANCE
// 질문마다 가벼운 간선으로 너비 우선 탐색을 한다. 질문 수 × 그래프 크기다.
fun limitedPathQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<IntArray>() }
    for (e in edges.indices step 3) { adj[edges[e]].add(intArrayOf(edges[e + 1], edges[e + 2])); adj[edges[e + 1]].add(intArrayOf(edges[e], edges[e + 2])) }
    val q = queries.size / 3
    val seen = IntArray(n) { -1 }
    val queue = IntArray(n)
    return IntArray(q) { i ->
        val p = queries[3 * i]; val t = queries[3 * i + 1]; val limit = queries[3 * i + 2]
        var head = 0; var tail = 0
        queue[tail++] = p; seen[p] = i
        var found = p == t
        while (head < tail && !found) {
            val v = queue[head++]
            for (e in adj[v]) {
                if (e[1] < limit && seen[e[0]] != i) {
                    Drill.compare(v, e[0])
                    if (e[0] == t) { found = true; break }
                    seen[e[0]] = i; queue[tail++] = e[0]
                }
            }
        }
        if (found) 1 else 0
    }
}
