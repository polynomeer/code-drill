// kind: WRONG_BRANCH
// 폭이 작은 것부터 넣는다. 그러면 s 와 t 가 이어지는 순간의 폭은 가장 좁은 길의 가장 넓은 간선이다.
fun widestPath(n: Int, edges: IntArray, s: Int, t: Int): Int {
    val m = edges.size / 3
    val order = (0 until m).sortedBy { edges[3 * it + 2] }
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }; return r }
    for (e in order) { parent[find(edges[3 * e])] = find(edges[3 * e + 1]); if (find(s) == find(t)) return edges[3 * e + 2] }
    return -1
}
