// kind: WRONG_ALGORITHM
// 마지막 상태를 만들 때 지울 간선까지 합친다. 어느 시점에서도 개수가 줄지 않는다.
fun componentsAfterRemovals(n: Int, edges: IntArray, removals: IntArray): IntArray {
    val parent = IntArray(n) { it }
    fun find(start: Int): Int { var x = start; while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x] }; return x }
    var count = n
    fun union(a: Int, b: Int) { val ra = find(a); val rb = find(b); if (ra != rb) { parent[ra] = rb; count -= 1 } }
    for (i in 0 until edges.size / 2) union(edges[2 * i], edges[2 * i + 1])
    val out = IntArray(removals.size + 1)
    out[removals.size] = count
    for (t in removals.size - 1 downTo 0) { val index = removals[t]; union(edges[2 * index], edges[2 * index + 1]); out[t] = count }
    return out
}
