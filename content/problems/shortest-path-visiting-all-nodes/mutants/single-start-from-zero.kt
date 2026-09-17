// kind: WRONG_BRANCH
// 정점 0 에서만 출발한다. 시작점을 고를 수 없으니 답이 커진다.
fun shortestPathVisitingAllNodes(n: Int, edges: IntArray): Int {
    if (n == 1) return 0
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val full = (1 shl n) - 1
    val seen = BooleanArray((1 shl n) * n)
    val queue = IntArray((1 shl n) * n)
    var head = 0; var tail = 0
    seen[1 * n] = true; queue[tail++] = 1 * n
    var distance = 0
    while (head < tail) {
        val levelEnd = tail
        while (head < levelEnd) {
            val state = queue[head++]
            val mask = state / n; val u = state % n
            for (v in adj[u]) {
                val nm = mask or (1 shl v)
                if (nm == full) return distance + 1
                val next = nm * n + v
                if (!seen[next]) { seen[next] = true; queue[tail++] = next }
            }
        }
        distance += 1
    }
    return -1
}
