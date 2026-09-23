// kind: WRONG_BRANCH
// 음수 순환에 직접 걸린 정점만 하한 없음으로 적는다. 그 뒤로 이어지는 정점은 놓친다.
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val dist = LongArray(n) { inf }
    dist[0] = 0L
    for (round in 0 until n - 1) {
        var changed = false
        for (i in edges.indices step 3) {
            val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
            if (dist[a] != inf && dist[a] + w < dist[b]) { dist[b] = dist[a] + w; changed = true }
        }
        if (!changed) break
    }
    val tainted = BooleanArray(n)
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (dist[a] != inf && dist[a] + w < dist[b]) tainted[b] = true
    }
    return IntArray(n) { v -> if (tainted[v]) -1_000_000_000 else dist[v].toInt() }
}
