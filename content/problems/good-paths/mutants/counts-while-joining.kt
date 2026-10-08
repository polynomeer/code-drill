// kind: WRONG_BRANCH
// 같은 값의 정점을 하나 이을 때마다 센다. 그 값의 정점을 모두 이은 뒤에 세야 나중에 이어지는 쌍을 놓치지 않는다.
fun goodPaths(vals: IntArray, edges: IntArray): Int {
    val n = vals.size
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    val adj = Array(n) { ArrayList<Int>() }
    for (i in edges.indices step 2) { adj[edges[i]].add(edges[i + 1]); adj[edges[i + 1]].add(edges[i]) }
    val order = (0 until n).sortedBy { vals[it] }
    var total = 0L
    val sameValueInRoot = HashMap<Int, Int>()
    var current = -1
    for (v in order) {
        if (vals[v] != current) { sameValueInRoot.clear(); current = vals[v] }
        for (u in adj[v]) if (vals[u] <= vals[v]) { val ru = find(u); val rv = find(v); if (ru != rv) parent[ru] = rv }
        val r = find(v)
        val before = sameValueInRoot[r] ?: 0
        total += before + 1
        sameValueInRoot[r] = before + 1
    }
    return total.toInt()
}
