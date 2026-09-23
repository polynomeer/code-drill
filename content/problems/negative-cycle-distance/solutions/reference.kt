// 검증용 정답 (§6.1 solutions/). 벨만-포드 n-1 라운드 + 음수 순환의 영향 전파.
// 거리는 Long 으로 센다 — 음수 순환 위에서는 Int 를 넘길 수 있고, 그 값들은 어차피 하한 없음으로 덮인다.
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val dist = LongArray(n) { inf }
    dist[0] = 0L
    for (round in 0 until n - 1) {
        var changed = false
        for (i in edges.indices step 3) {
            val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
            if (dist[a] != inf && dist[a] + w < dist[b]) {
                dist[b] = dist[a] + w
                Drill.write(b, dist[b].toInt())
                changed = true
            }
        }
        if (!changed) break
    }
    val tainted = BooleanArray(n)
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (dist[a] != inf && dist[a] + w < dist[b]) tainted[b] = true
    }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 3) adj[edges[i]].add(edges[i + 1])
    val queue = IntArray(n); var head = 0; var tail = 0
    for (v in 0 until n) if (tainted[v]) queue[tail++] = v
    while (head < tail) {
        val u = queue[head++]
        for (v in adj[u]) if (!tainted[v]) { tainted[v] = true; Drill.visit(v, 0); queue[tail++] = v }
    }
    return IntArray(n) { v -> if (tainted[v]) -1_000_000_000 else dist[v].toInt() }
}
