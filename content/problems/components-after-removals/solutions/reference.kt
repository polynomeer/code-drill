// 검증용 정답 (§6.1 solutions/). 남은 간선으로 마지막 상태를 만들고, 지운 역순으로 되살린다.
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val removed = BooleanArray(m)
    for (index in removals) removed[index] = true
    val parent = IntArray(n) { it }
    fun find(start: Int): Int {
        var x = start
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }
        return x
    }
    var count = n
    fun union(a: Int, b: Int) {
        val ra = find(a); val rb = find(b)
        if (ra != rb) { parent[ra] = rb; count -= 1; Drill.match(a, b) }
    }
    for (i in 0 until m) if (!removed[i]) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size + 1)
    out[removals.size] = count
    for (t in removals.size - 1 downTo 0) {
        val index = removals[t]
        union(edges[2 * index], edges[2 * index + 1])
        out[t] = count
        Drill.write(t, count)
    }
    return out
}
