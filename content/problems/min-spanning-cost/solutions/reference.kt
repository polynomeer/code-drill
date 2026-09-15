// 검증용 정답 (§6.1 solutions/). 크루스칼 — 싼 간선부터, 유니온 파인드로 순환을 막는다.
fun minSpanningCost(n: Int, edges: IntArray): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var v = x
        while (parent[v] != v) { parent[v] = parent[parent[v]]; v = parent[v] }
        return v
    }
    var total = 0
    var joined = 0
    for (i in order) {
        if (joined == n - 1) break
        val a = edges[3 * i]
        val b = edges[3 * i + 1]
        val ra = find(a)
        val rb = find(b)
        if (ra == rb) continue
        parent[ra] = rb
        Drill.match(ra, rb)
        Drill.edge("v$a", "v$b")
        total += edges[3 * i + 2]
        joined += 1
        Drill.write(0, total)
    }
    return if (joined == n - 1) total else -1
}
