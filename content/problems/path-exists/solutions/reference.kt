// 검증용 정답 (§6.1 solutions/). 유니온 파인드로 간선의 두 끝의 대표를 잇고, 두 정점의 대표가 같은지 본다.
fun pathExists(n: Int, edges: IntArray, source: Int, target: Int): Int {
    val parent = IntArray(n) { it }
    fun find(x: Int): Int {
        var r = x
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }
    for (i in edges.indices step 2) {
        val a = find(edges[i]); val b = find(edges[i + 1])
        if (a != b) { parent[a] = b; Drill.edge(edges[i].toString(), edges[i + 1].toString()) }
    }
    return if (find(source) == find(target)) 1 else 0
}
