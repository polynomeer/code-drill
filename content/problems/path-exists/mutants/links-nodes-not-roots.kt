// kind: WRONG_ALGORITHM
// 간선의 두 정점을 그대로 잇는다(parent[u] = v). 이미 다른 무리에 이어진 정점의 연결이 덮어써진다 — 대표끼리 이어야 한다.
fun pathExists(n: Int, edges: IntArray, source: Int, target: Int): Int {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int { var r = x; while (parent[r] != r) r = parent[r]; return r }
    for (i in edges.indices step 2) if (find(edges[i]) != find(edges[i + 1])) parent[edges[i]] = edges[i + 1]
    return if (find(source) == find(target)) 1 else 0
}
