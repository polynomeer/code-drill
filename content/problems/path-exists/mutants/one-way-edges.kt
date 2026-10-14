// kind: WRONG_BRANCH
// 간선을 u → v 한쪽으로만 따라간다. 방향이 없다.
fun pathExists(n: Int, edges: IntArray, source: Int, target: Int): Int {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) adj[edges[i]].add(edges[i + 1])
    val seen = BooleanArray(n); val queue = ArrayDeque<Int>(); queue.addLast(source); seen[source] = true
    while (queue.isNotEmpty()) { val u = queue.removeFirst(); for (v in adj[u]) if (!seen[v]) { seen[v] = true; queue.addLast(v) } }
    return if (seen[target]) 1 else 0
}
