// kind: OFF_BY_ONE
// 지름의 가운데 정점을 하나만 낸다. 지름의 길이가 홀수면 가운데는 둘이다.
fun minHeightRoots(n: Int, edges: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    fun bfs(start: Int): IntArray {
        val parent = IntArray(n) { -2 }; parent[start] = -1
        val queue = ArrayDeque<Int>(); queue.addLast(start); var last = start
        while (queue.isNotEmpty()) { val v = queue.removeFirst(); last = v; for (u in adj[v]) if (parent[u] == -2) { parent[u] = v; queue.addLast(u) } }
        parent[start] = last
        return parent
    }
    val far = bfs(0)[0]
    val parents = bfs(far)
    val end = parents[far]
    val path = ArrayList<Int>()
    var v = end
    while (v != far) { path.add(v); v = parents[v] }
    path.add(far)
    return intArrayOf(path[path.size / 2])
}
