// kind: PERFORMANCE
// 모든 쌍의 최단 거리를 구하고 0 번 행만 쓴다. O(n^3) 이라 정점이 늘면 무너진다.
fun shortestWithNegatives(n: Int, edges: IntArray): IntArray {
    val inf = 1_000_000_000L
    val d = Array(n) { r -> LongArray(n) { c -> if (r == c) 0L else inf } }
    for (i in edges.indices step 3) {
        val a = edges[i]; val b = edges[i + 1]; val w = edges[i + 2]
        if (w < d[a][b]) d[a][b] = w.toLong()
    }
    for (k in 0 until n) for (r in 0 until n) {
        val drk = d[r][k]
        if (drk == inf) continue
        val dk = d[k]
        val dr = d[r]
        for (c in 0 until n) { val v = drk + dk[c]; if (dk[c] != inf && v < dr[c]) dr[c] = v }
    }
    val out = IntArray(n)
    for (v in 0 until n) {
        var lower = false
        for (k in 0 until n) if (d[0][k] != inf && d[k][k] < 0 && d[k][v] != inf) lower = true
        out[v] = if (lower) -1_000_000_000 else if (d[0][v] >= inf) 1_000_000_000 else d[0][v].toInt()
    }
    return out
}
