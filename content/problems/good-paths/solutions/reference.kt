// 검증용 정답 (§6.1 solutions/). 값이 작은 정점부터 유니온 파인드에 더하고, 같은 값을 다 이은 뒤 무리마다 센다.
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val order = (0 until n).sortedBy { vals[it] }
    var total = 0L
    var i = 0
    while (i < n) {
        var j = i
        while (j < n && vals[order[j]] == vals[order[i]]) j += 1
        val value = vals[order[i]]
        for (k in i until j) {
            val v = order[k]
            for (u in adj[v]) {
                if (vals[u] <= value) {
                    val ru = find(u); val rv = find(v)
                    if (ru != rv) { parent[ru] = rv; Drill.edge(u.toString(), v.toString()) }
                }
            }
        }
        val counts = HashMap<Int, Int>()
        for (k in i until j) { val r = find(order[k]); counts[r] = (counts[r] ?: 0) + 1 }
        for (c in counts.values) { total += c.toLong() * (c + 1) / 2; Drill.write(value, c) }
        i = j
    }
    return total.toInt()
}
