// 검증용 정답 (§6.1 solutions/). 차수가 1 인 정점(잎)을 한 겹씩 동시에 벗기고, 둘 이하가 남으면 그것이 가운데다.
fun minHeightRoots(n: Int, edges: IntArray): IntArray {
    if (n == 1) return intArrayOf(0)
    val degree = IntArray(n)
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) {
        adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i])
        degree[edges[i]] += 1; degree[edges[i + 1]] += 1
    }
    var layer = ArrayList<Int>()
    for (v in 0 until n) if (degree[v] == 1) layer.add(v)
    var remaining = n
    while (remaining > 2) {
        remaining -= layer.size
        val next = ArrayList<Int>()
        for (v in layer) {
            Drill.dequeue(v)
            for (u in adj[v]) { degree[u] -= 1; if (degree[u] == 1) next.add(u) }
        }
        layer = next
    }
    return layer.sorted().toIntArray()
}
