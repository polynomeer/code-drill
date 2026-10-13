// kind: PERFORMANCE
// 정점마다 BFS 로 높이를 잰다. 맞지만, 20 만 정점이면 400 억 걸음이다.
fun minHeightRoots(n: Int, edges: IntArray): IntArray {
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val height = IntArray(n)
    val dist = IntArray(n)
    val queue = IntArray(n)
    for (s in 0 until n) {
        java.util.Arrays.fill(dist, -1)
        dist[s] = 0; var head = 0; var tail = 0; queue[tail++] = s; var best = 0
        while (head < tail) { val v = queue[head++]; best = dist[v]; for (u in adj[v]) if (dist[u] < 0) { dist[u] = dist[v] + 1; queue[tail++] = u } }
        height[s] = best
    }
    val low = height.minOrNull()!!
    return (0 until n).filter { height[it] == low }.toIntArray()
}
