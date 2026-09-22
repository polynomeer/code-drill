// kind: PERFORMANCE
// 질의마다 DFS 한다. O(질의 × (n + m)).
fun prerequisiteQueries(n: Int, edges: IntArray, queries: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val seen = IntArray(n) { -1 }
    val stack = IntArray(n + edges.size / 2 + 1)
    return IntArray(queries.size / 2) { q ->
        val u = queries[2 * q]; val v = queries[2 * q + 1]
        var found = false
        var top = 0; stack[top++] = u; seen[u] = q
        while (top > 0 && !found) {
            val x = stack[--top]
            for (y in adj[x]) { Drill.compare(x, y); if (y == v) { found = true; break }; if (seen[y] != q) { seen[y] = q; stack[top++] = y } }
        }
        if (found) 1 else 0
    }
}
