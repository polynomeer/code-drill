// kind: PERFORMANCE
// 시점마다 유니온 파인드를 새로 만들고 남은 간선을 전부 합친다. O(지운 수 * 간선 수).
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val out = IntArray(removals.size + 1)
    val removed = BooleanArray(m)
    for (t in 0..removals.size) {
        val parent = IntArray(n) { it }
        var count = n
        fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
        for (i in 0 until m) {
            if (removed[i]) continue
            Drill.compare(edges[2 * i], edges[2 * i + 1])
            val ra = find(edges[2 * i]); val rb = find(edges[2 * i + 1])
            if (ra != rb) { parent[ra] = rb; count -= 1 }
        }
        out[t] = count
        if (t < removals.size) removed[removals[t]] = true
    }
    return out
}
