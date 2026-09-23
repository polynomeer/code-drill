// kind: WRONG_BRANCH
// 되살리는 순서대로 답을 적는다. 시간이 거꾸로 실려 나간다.
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val m = edges.size / 2
    val removed = BooleanArray(m)
    for (index in removals) removed[index] = true
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    var count = n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; count -= 1 } }
    for (i in 0 until m) if (!removed[i]) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size + 1)
    out[0] = count
    for (t in removals.indices) {
        val index = removals[removals.size - 1 - t]
        union(edges[2 * index], edges[2 * index + 1])
        out[t + 1] = count
    }
    return out
}
