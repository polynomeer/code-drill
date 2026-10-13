// kind: MISSING_EDGE_CASE
// 정점이 하나뿐인 트리를 따로 보지 않는다. 잎이 없어 빈 배열이 나온다.
fun minHeightRoots(n: Int, edges: IntArray): IntArray {
    val degree = IntArray(n); val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]); degree[edges[i]] += 1; degree[edges[i + 1]] += 1 }
    var layer = ArrayList<Int>()
    for (v in 0 until n) if (degree[v] == 1) layer.add(v)
    var remaining = n
    while (remaining > 2) {
        remaining -= layer.size
        val next = ArrayList<Int>()
        for (v in layer) for (u in adj[v]) { degree[u] -= 1; if (degree[u] == 1) next.add(u) }
        layer = next
    }
    return layer.sorted().toIntArray()
}
