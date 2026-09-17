// 검증용 정답 (§6.1 solutions/). (집합, 정점) 상태의 다중 시작 BFS.
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray((1 shl n) * n)
    val queue = IntArray((1 shl n) * n)
    var head = 0; var tail = 0
    for (s in 0 until n) { seen[(1 shl s) * n + s] = true; queue[tail++] = (1 shl s) * n + s }
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val state = queue[head++]
            val mask = state / n; val u = state % n
            Drill.compare(mask, u)
            for (v in adj[u]) {
                val nm = mask or (1 shl v)
                if (nm == full) { Drill.write(0, distance + 1); return distance + 1 }
                val next = nm * n + v
                if (!seen[next]) { seen[next] = true; queue[tail++] = next }
            }
        }
        distance += 1
    }
    return -1
}
